package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.control_flow.IfStatement
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.*
import logger.issues.constant_conditions.*
import components.syntax_parser.syntax_tree.operations.Cast as CastSyntaxTree

class Cast(override val source: CastSyntaxTree, scope: Scope, val value: Value, val variableDeclaration: ValueDeclaration?,
		   val referenceType: Type, val operator: Operator): Value(source, scope) {
	override var isInterruptingExecution = false
	private val isCastAlwaysSuccessful: Boolean
		get() = (value.staticValue?.type ?: value.type)?.isAssignableTo(referenceType) ?: false
	private val isCastNeverSuccessful: Boolean
		get() = value.staticValue is NullLiteral

	init {
		addUnits(value, variableDeclaration)
		type = if(operator.returnsBoolean) {
			addUnits(referenceType)
			LiteralType(source, scope, Linter.SpecialType.BOOLEAN)
		} else if(operator == Operator.OPTIONAL_CAST) {
			OptionalType(source, scope, referenceType)
		} else {
			referenceType
		}
		addUnits(type)
	}

	override fun linkTypes(linter: Linter) {
		super.linkTypes(linter)
		variableDeclaration?.type = referenceType
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		if(operator.returnsBoolean) {
			if(isCastAlwaysSuccessful)
				staticValue = BooleanLiteral(source, scope, operator == Operator.CAST_CONDITION, linter)
			else if(isCastNeverSuccessful)
				staticValue = BooleanLiteral(source, scope, operator == Operator.NEGATED_CAST_CONDITION, linter)
		} else if(operator == Operator.SAFE_CAST) {
			staticValue = value.staticValue
		} else if(operator == Operator.THROWING_CAST) {
			staticValue = value.staticValue
			isInterruptingExecution = isCastNeverSuccessful //TODO propagate 'isInterruptingExecution' property from expressions to statements in the 'Unit' class
		} else if(operator == Operator.OPTIONAL_CAST) {
			if(isCastAlwaysSuccessful)
				staticValue = value.staticValue
			else if(isCastNeverSuccessful)
				staticValue = NullLiteral(source, scope, linter)
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		super.analyseDataFlow(linter, tracker)
		if(operator.returnsBoolean) {
			val variableValue = value as? VariableValue
			val declaration = variableValue?.definition
			if(declaration != null) {
				val commonState = tracker.currentState.copy()
				tracker.add(VariableUsage.Kind.HINT, declaration, this, referenceType)
				setEndState(tracker, operator == Operator.CAST_CONDITION)
				tracker.setVariableStates(commonState)
				val variableType = variableValue.type as? OptionalType
				val baseType = variableType?.baseType
				if(baseType == referenceType) {
					val nullLiteral = NullLiteral(source, scope, linter)
					tracker.add(VariableUsage.Kind.HINT, declaration, this, nullLiteral.type, nullLiteral)
					setEndState(tracker, operator == Operator.NEGATED_CAST_CONDITION)
				}
				tracker.setVariableStates(commonState)
			}
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		value.type?.let { valueType ->
			if(valueType.isAssignableTo(referenceType)) {
				if(operator.isConditional)
					linter.addIssue(ConditionalCastIsSafe(source, valueType, referenceType))
			} else {
				if(!operator.isConditional)
					linter.addIssue(UnsafeSafeCast(source, valueType, referenceType))
			}
		}
		validateVariableDeclaration(linter)
	}

	private fun validateVariableDeclaration(linter: Linter) {
		if(variableDeclaration == null)
			return
		if(!operator.returnsBoolean) {
			linter.addIssue(CastVariableWithoutIs(source))
			return
		}
		val ifStatement = parent as? IfStatement
		if(ifStatement == null) {
			linter.addIssue(CastVariableOutsideOfIfStatement(source))
			return
		}
		//TODO handle this using data-flow instead
		val isVariableAccessibleAfterIfStatement =
			(operator == Operator.CAST_CONDITION && ifStatement.negativeBranch?.isInterruptingExecution == true)
				|| (operator == Operator.NEGATED_CAST_CONDITION && ifStatement.positiveBranch.isInterruptingExecution)
		for(usage in variableDeclaration.usages) {
			if(ifStatement.positiveBranch.contains(usage)) {
				if(operator == Operator.NEGATED_CAST_CONDITION)
					linter.addIssue(NegatedCastVariableAccessInPositiveBranch(usage.source))
			} else if(ifStatement.negativeBranch?.contains(usage) == true) {
				if(operator == Operator.CAST_CONDITION)
					linter.addIssue(CastVariableAccessInNegativeBranch(usage.source))
			} else {
				if(!isVariableAccessibleAfterIfStatement)
					linter.addIssue(CastVariableAccessAfterIfStatement(usage.source))
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
