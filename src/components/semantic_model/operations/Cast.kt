package components.semantic_model.operations

import components.code_generation.llvm.models.operations.Cast
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.control_flow.IfExpression
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.types.LiteralType
import components.semantic_model.types.OptionalType
import components.semantic_model.types.Type
import components.semantic_model.values.*
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
		variableDeclaration?.providedType = referenceType
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

	override fun toUnit() = Cast(this, subject.toUnit(), variableDeclaration?.toUnit())

	enum class Operator(val stringRepresentation: String, val isConditional: Boolean = false,
						val returnsBoolean: Boolean = false) {
		SAFE_CAST("as"),
		OPTIONAL_CAST("as?", true),
		RAISING_CAST("as!", true),
		CAST_CONDITION("is", true, true),
		NEGATED_CAST_CONDITION("is!", true, true)
	}
}
