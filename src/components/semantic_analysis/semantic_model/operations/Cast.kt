package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.control_flow.IfStatement
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.*
import messages.Message
import components.syntax_parser.syntax_tree.operations.Cast as CastSyntaxTree

class Cast(override val source: CastSyntaxTree, val value: Value, val variableDeclaration: ValueDeclaration?,
		   val referenceType: Type, val operator: Operator): Value(source) {
	override var isInterruptingExecution = false
	private val isCastAlwaysSuccessful: Boolean
		get() = (value.staticValue?.type ?: value.type)?.isAssignableTo(referenceType) ?: false
	private val isCastNeverSuccessful: Boolean
		get() = value.staticValue is NullLiteral

	init {
		addUnits(value, variableDeclaration)
		type = if(operator.returnsBoolean) {
			addUnits(referenceType)
			LiteralType(source, Linter.SpecialType.BOOLEAN)
		} else if(operator == Operator.OPTIONAL_CAST) {
			OptionalType(source, referenceType)
		} else {
			referenceType
		}
		addUnits(type)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, scope)
		variableDeclaration?.type = referenceType
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		if(operator.returnsBoolean) {
			if(isCastAlwaysSuccessful)
				staticValue = BooleanLiteral(source, operator == Operator.CAST_CONDITION, linter)
			else if(isCastNeverSuccessful)
				staticValue = BooleanLiteral(source, operator == Operator.NEGATED_CAST_CONDITION, linter)
		} else if(operator == Operator.SAFE_CAST) {
			staticValue = value.staticValue
		} else if(operator == Operator.THROWING_CAST) {
			staticValue = value.staticValue
			isInterruptingExecution = isCastNeverSuccessful //TODO propagate 'isInterruptingExecution' property from expressions to statements in the 'Unit' class
		} else if(operator == Operator.OPTIONAL_CAST) {
			if(isCastAlwaysSuccessful)
				staticValue = value.staticValue
			else if(isCastNeverSuccessful)
				staticValue = NullLiteral(source, linter)
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		value.type?.let { valueType ->
			if(valueType.isAssignableTo(referenceType)) {
				if(operator.isConditional)
					linter.addMessage(source, "Cast from '$valueType' to '$referenceType' is safe.", Message.Type.WARNING)
			} else {
				if(!operator.isConditional)
					linter.addMessage(source, "Cannot safely cast '$valueType' to '$referenceType'.", Message.Type.ERROR)
			}
		}
		validateVariableDeclaration(linter)
	}

	private fun validateVariableDeclaration(linter: Linter) {
		if(variableDeclaration == null)
			return
		if(!operator.returnsBoolean) {
			linter.addMessage(source, "Only 'is' casts can declare a variable.", Message.Type.WARNING)
			return
		}
		val ifStatement = parent as? IfStatement
		if(ifStatement == null) {
			linter.addMessage(source, "'is' casts can only declare a variable in an if statement condition.",
				Message.Type.WARNING)
			return
		}
		val isVariableAccessibleAfterIfStatement =
			(operator == Operator.CAST_CONDITION && ifStatement.negativeBranch?.isInterruptingExecution == true)
				|| (operator == Operator.NEGATED_CAST_CONDITION && ifStatement.positiveBranch.isInterruptingExecution)
		for(usage in variableDeclaration.usages) {
			if(ifStatement.positiveBranch.contains(usage)) {
				if(operator == Operator.NEGATED_CAST_CONDITION)
					linter.addMessage(usage.source, "Cannot access negated cast variable in positive branch.", Message.Type.ERROR)
			} else if(ifStatement.negativeBranch?.contains(usage) == true) {
				if(operator == Operator.CAST_CONDITION)
					linter.addMessage(usage.source, "Cannot access cast variable in negative branch.", Message.Type.ERROR)
			} else {
				if(!isVariableAccessibleAfterIfStatement)
					linter.addMessage(usage.source, "Cannot access cast variable after if statement.", Message.Type.ERROR)
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
