package components.semantic_model.operations

import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.control_flow.IfStatement
import components.semantic_model.scopes.Scope
import components.semantic_model.types.LiteralType
import components.semantic_model.types.OptionalType
import components.semantic_model.types.Type
import components.semantic_model.values.*
import logger.issues.constant_conditions.*
import components.syntax_parser.syntax_tree.operations.Cast as CastSyntaxTree

class Cast(override val source: CastSyntaxTree, scope: Scope, val subject: Value, val variableDeclaration: ValueDeclaration?,
		   val referenceType: Type, val operator: Operator): Value(source, scope) {
	override var isInterruptingExecution = false
	private val isCastAlwaysSuccessful: Boolean
		get() = subject.getComputedType()?.isAssignableTo(referenceType) ?: false
	private val isCastNeverSuccessful: Boolean
		get() = subject.getComputedValue() is NullLiteral

	init {
		addSemanticModels(subject, variableDeclaration)
		type = if(operator.returnsBoolean) {
			addSemanticModels(referenceType)
			LiteralType(source, scope, SpecialType.BOOLEAN)
		} else if(operator == Operator.OPTIONAL_CAST) {
			OptionalType(source, scope, referenceType)
		} else {
			referenceType
		}
		addSemanticModels(type)
	}

	override fun determineTypes() {
		super.determineTypes()
		variableDeclaration?.type = referenceType
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		if(operator.returnsBoolean) {
			val subjectVariable = subject as? VariableValue
			val subjectVariableDeclaration = subjectVariable?.declaration
			if(subjectVariableDeclaration != null) {
				val commonState = tracker.currentState.copy()
				tracker.add(VariableUsage.Kind.HINT, subjectVariableDeclaration, this, referenceType)
				setEndState(tracker, operator == Operator.CAST_CONDITION)
				tracker.setVariableStates(commonState)
				val variableType = subjectVariable.type as? OptionalType
				val baseType = variableType?.baseType
				if(baseType == referenceType) {
					val nullLiteral = NullLiteral(this)
					tracker.add(VariableUsage.Kind.HINT, subjectVariableDeclaration, this, nullLiteral.type, nullLiteral)
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
		} else if(operator == Operator.THROWING_CAST) {
			staticValue = subject.getComputedValue()
			//TODO propagate 'isInterruptingExecution' property from expressions to statements in the 'SemanticModel' class
			isInterruptingExecution = isCastNeverSuccessful
		} else if(operator == Operator.OPTIONAL_CAST) {
			if(isCastAlwaysSuccessful)
				staticValue = subject.getComputedValue()
			else if(isCastNeverSuccessful)
				staticValue = NullLiteral(this)
		}
	}

	override fun validate() {
		super.validate()
		subject.type?.let { valueType ->
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
		val ifStatement = parent as? IfStatement
		if(ifStatement == null) {
			context.addIssue(CastVariableOutsideOfIfStatement(source))
			return
		}
		//TODO handle this using data-flow instead
		val isVariableAccessibleAfterIfStatement =
			(operator == Operator.CAST_CONDITION && ifStatement.negativeBranch?.isInterruptingExecution == true)
				|| (operator == Operator.NEGATED_CAST_CONDITION && ifStatement.positiveBranch.isInterruptingExecution)
		for(usage in variableDeclaration.usages) {
			if(ifStatement.positiveBranch.contains(usage)) {
				if(operator == Operator.NEGATED_CAST_CONDITION)
					context.addIssue(NegatedCastVariableAccessInPositiveBranch(usage.source))
			} else if(ifStatement.negativeBranch?.contains(usage) == true) {
				if(operator == Operator.CAST_CONDITION)
					context.addIssue(CastVariableAccessInNegativeBranch(usage.source))
			} else {
				if(!isVariableAccessibleAfterIfStatement)
					context.addIssue(CastVariableAccessAfterIfStatement(usage.source))
			}
		}
	}

	enum class Operator(val stringRepresentation: String, val isConditional: Boolean = false,
						val returnsBoolean: Boolean = false) {
		SAFE_CAST("as"),
		OPTIONAL_CAST("as?", true),
		THROWING_CAST("as!", true),
		CAST_CONDITION("is", true, true),
		NEGATED_CAST_CONDITION("is!", true, true)
	}
}
