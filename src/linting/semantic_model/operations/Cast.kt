package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.OptionalType
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import messages.Message
import parsing.syntax_tree.operations.Cast

class Cast(override val source: Cast, val value: Value, val variable: VariableValue?, val referenceType: Type,
		   val operator: Operator): Value(source) {

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