package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.OptionalType
import linting.semantic_model.types.Type
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.BooleanLiteral
import linting.semantic_model.values.NullLiteral
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import messages.Message
import parsing.syntax_tree.operations.Cast

class Cast(override val source: Cast, val value: Value, val variable: VariableValue?, val referenceType: Type,
		   val operator: Operator): Value(source) {
	override var isInterruptingExecution = false
	private val isCastAlwaysSuccessful: Boolean
		get() = (value.staticValue?.type ?: value.type)?.isAssignableTo(referenceType) ?: false
	private val isCastNeverSuccessful: Boolean
		get() = value.staticValue is NullLiteral

	init {
		units.add(value)
		if(variable != null)
			units.add(variable)
		type = if(operator.returnsBoolean) {
			units.add(referenceType)
			ObjectType(source, Linter.LiteralType.BOOLEAN.className)
		} else if(operator == Operator.OPTIONAL_CAST) {
			val type = OptionalType(source, referenceType)
			units.add(type)
			type
		} else {
			units.add(referenceType)
			referenceType
		}
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, scope)
		if(operator.returnsBoolean)
			linter.link(Linter.LiteralType.BOOLEAN, type)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		if(operator.returnsBoolean) {
			if(isCastAlwaysSuccessful)
				staticValue = BooleanLiteral(source, operator == Operator.CAST_CONDITION)
			else if(isCastNeverSuccessful)
				staticValue = BooleanLiteral(source, operator == Operator.NEGATED_CAST_CONDITION)
		} else if(operator == Operator.SAFE_CAST) {
			staticValue = value.staticValue
		} else if(operator == Operator.THROWING_CAST) {
			staticValue = value.staticValue
			isInterruptingExecution = isCastNeverSuccessful //TODO propagate 'isInterruptingExecution' property from expressions to statements in the 'Unit' class
		} else if(operator == Operator.OPTIONAL_CAST) {
			if(isCastAlwaysSuccessful)
				staticValue = value.staticValue
			else if(isCastNeverSuccessful)
				staticValue = NullLiteral(source)
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		value.type?.let { valueType ->
			if(valueType.isAssignableTo(referenceType)) {
				if(operator.isConditional)
					linter.addMessage(source, "Cast from '$valueType' to '$referenceType' is safe.",
						Message.Type.WARNING)
			} else {
				if(!operator.isConditional)
					linter.addMessage(source, "Cannot safely cast '$valueType' to '$referenceType'.",
						Message.Type.ERROR)
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
