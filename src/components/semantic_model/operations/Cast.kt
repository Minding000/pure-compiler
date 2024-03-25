package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.ValueConverter
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.control_flow.IfExpression
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.types.*
import components.semantic_model.values.*
import errors.internal.CompilerError
import logger.issues.constant_conditions.*
import components.syntax_parser.syntax_tree.operations.Cast as CastSyntaxTree

class Cast(override val source: CastSyntaxTree, scope: Scope, val subject: Value, val variableDeclaration: ValueDeclaration?,
		   val referenceType: Type, val operator: Operator): Value(source, scope) {
	override var isInterruptingExecutionBasedOnStaticEvaluation = false
	private val isCastAlwaysSuccessful: Boolean
		get() = subject.getComputedType()?.isAssignableTo(referenceType) ?: false
	private val isCastNeverSuccessful: Boolean
		get() = subject.getComputedValue() is NullLiteral

	init {
		addSemanticModels(subject, variableDeclaration)
		providedType = if(operator.returnsBoolean) {
			addSemanticModels(referenceType)
			LiteralType(source, scope, SpecialType.BOOLEAN)
		} else if(operator == Operator.OPTIONAL_CAST) {
			OptionalType(source, scope, referenceType)
		} else {
			referenceType
		}
		addSemanticModels(providedType)
	}

	override fun determineTypes() {
		variableDeclaration?.type = referenceType
		super.determineTypes()
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		subject.analyseDataFlow(tracker)
		if(variableDeclaration != null)
			tracker.declare(variableDeclaration, true)
		setEndStates(tracker)
		if(operator.returnsBoolean) {
			val subjectVariable = subject as? VariableValue
			val subjectVariableDeclaration = subjectVariable?.declaration
			if(subjectVariableDeclaration != null) {
				val commonState = tracker.currentState.copy()
				tracker.add(VariableUsage.Kind.HINT, subjectVariableDeclaration, this, referenceType)
				setEndState(tracker, operator == Operator.CAST_CONDITION)
				tracker.setVariableStates(commonState)
				val variableType = subjectVariable.providedType as? OptionalType
				val baseType = variableType?.baseType
				if(baseType == referenceType) {
					val nullLiteral = NullLiteral(this)
					tracker.add(VariableUsage.Kind.HINT, subjectVariableDeclaration, this, nullLiteral.providedType, nullLiteral)
					setEndState(tracker, operator == Operator.NEGATED_CAST_CONDITION)
				}
				tracker.setVariableStates(commonState)
			}
		}
		computeStaticValue()
	}

	private fun computeStaticValue() {
		if(operator.returnsBoolean) {
			if(isCastAlwaysSuccessful)
				staticValue = BooleanLiteral(this, operator == Operator.CAST_CONDITION)
			else if(isCastNeverSuccessful)
				staticValue = BooleanLiteral(this, operator == Operator.NEGATED_CAST_CONDITION)
		} else if(operator == Operator.SAFE_CAST) {
			staticValue = subject.getComputedValue()
		} else if(operator == Operator.RAISING_CAST) {
			staticValue = subject.getComputedValue()
			//TODO propagate 'isInterruptingExecution' property from expressions to statements in the 'SemanticModel' class
			isInterruptingExecutionBasedOnStaticEvaluation = isCastNeverSuccessful
		} else if(operator == Operator.OPTIONAL_CAST) {
			if(isCastAlwaysSuccessful)
				staticValue = subject.getComputedValue()
			else if(isCastNeverSuccessful)
				staticValue = NullLiteral(this)
		}
	}

	override fun validate() {
		super.validate()
		subject.providedType?.let { valueType ->
			if(valueType.isAssignableTo(referenceType)) {
				if(operator.isConditional)
					context.addIssue(ConditionalCastIsSafe(source, valueType, referenceType))
			} else {
				if(!operator.isConditional)
					context.addIssue(UnsafeSafeCast(source, valueType, referenceType))
			}
		}
		validateVariableDeclaration()
	}

	private fun validateVariableDeclaration() {
		if(variableDeclaration == null)
			return
		if(!operator.returnsBoolean) {
			context.addIssue(CastVariableWithoutIs(source))
			return
		}
		val ifExpression = parent as? IfExpression
		if(ifExpression == null) {
			context.addIssue(CastVariableOutsideOfIfStatement(source))
			return
		}
		//TODO handle this using data-flow instead
		val isVariableAccessibleAfterIfStatement =
			(operator == Operator.CAST_CONDITION && ifExpression.negativeBranch?.isInterruptingExecutionBasedOnStructure == true)
				|| (operator == Operator.NEGATED_CAST_CONDITION && ifExpression.positiveBranch.isInterruptingExecutionBasedOnStructure)
		for(usage in variableDeclaration.usages) {
			if(ifExpression.positiveBranch.contains(usage)) {
				if(operator == Operator.NEGATED_CAST_CONDITION)
					context.addIssue(NegatedCastVariableAccessInPositiveBranch(usage.source))
			} else if(ifExpression.negativeBranch?.contains(usage) == true) {
				if(operator == Operator.CAST_CONDITION)
					context.addIssue(CastVariableAccessInNegativeBranch(usage.source))
			} else {
				if(!isVariableAccessibleAfterIfStatement)
					context.addIssue(CastVariableAccessAfterIfStatement(usage.source))
			}
		}
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {

		//TODO implement special cases:
		// - Cast from optional primitive to primitive: unbox and check
		// - Cast from primitive to optional primitive: box
		// - Cast from primitive to pointer type: construct wrapper
		// - Cast from pointer type to primitive: destruct wrapper
		// -
		// - Cast from primitive to primitive: LLVM cast (could be combined with casts above)
		// - Cast null value (no type info)


		val subjectValue = subject.getLlvmValue(constructor)
		when(operator) {
			Operator.SAFE_CAST -> {
				return ValueConverter.convertIfRequired(this, constructor, subjectValue, subject.providedType, referenceType)
			}
			Operator.OPTIONAL_CAST -> {
				//TODO check if value can be converted
				// else: return null pointer
				return ValueConverter.convertIfRequired(this, constructor, subjectValue, subject.providedType, referenceType)
			}
			Operator.RAISING_CAST -> {
				//TODO check if value can be converted
				// else: raise
				return ValueConverter.convertIfRequired(this, constructor, subjectValue, subject.providedType, referenceType)
			}
			Operator.CAST_CONDITION, Operator.NEGATED_CAST_CONDITION -> {
				//TODO check if value can be converted
				// - get specified type (is class definition address)
				// - get value
				// - check if values class definition matches specified class definition
				//   - if yes: result = true
				// - else:
				//   - get parent class definitions => need to be part of definition
				//   - recursively check is match
				//     - if yes for any: result = true
				// - fallback: result = false
				// => advanced feature: support complex types

				//TODO if subject is primitive:
				// - skip comparison
				// - statically determine result

				val subjectClassDefinition = context.getClassDefinition(constructor, subjectValue)
				val referenceTypeDeclaration = when(referenceType) {
					is ObjectType -> referenceType.getTypeDeclaration()
					is SelfType -> referenceType.typeDeclaration
					else -> throw CompilerError(referenceType.source,
						"Conditional casts do not support complex types at the moment. Provided type: $referenceType")
				}
				val referenceClassDefinition = referenceTypeDeclaration?.llvmClassDefinition
					?: throw CompilerError(referenceType.source, "Missing class definition for '$referenceType'.")
				if(variableDeclaration != null) {
					variableDeclaration.compile(constructor)
					constructor.buildStore(subjectValue, variableDeclaration.llvmLocation)
				}
				val resultName = "_castConditionResult"
				return if(operator == Operator.CAST_CONDITION)
					constructor.buildPointerEqualTo(subjectClassDefinition, referenceClassDefinition, resultName)
				else
					constructor.buildPointerNotEqualTo(subjectClassDefinition, referenceClassDefinition, resultName)
			}
		}
	}

	enum class Operator(val stringRepresentation: String, val isConditional: Boolean = false,
						val returnsBoolean: Boolean = false) {
		SAFE_CAST("as"),
		OPTIONAL_CAST("as?", true),
		RAISING_CAST("as!", true),
		CAST_CONDITION("is", true, true),
		NEGATED_CAST_CONDITION("is!", true, true)
	}
}
