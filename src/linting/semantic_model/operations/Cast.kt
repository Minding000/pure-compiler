package linting.semantic_model.operations

import linting.semantic_model.literals.Type
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import parsing.syntax_tree.operations.Cast

class Cast(override val source: Cast, val value: Value, val variable: VariableValue?, type: Type, val operator: Operator):
	Value(source) {

	init {
		units.add(value)
		if(variable != null)
			units.add(variable)
		units.add(type)
		this.type = if(operator.isBoolean)
			TODO("Reference native boolean type")
		else
			type
	}

	enum class Operator(val stringRepresentation: String, val isBoolean: Boolean = false) {
		SAFE_CAST("as"),
		OPTIONAL_CAST("as?"),
		THROWING_CAST("as!"),
		CAST_CONDITION("is", true),
		NEGATED_CAST_CONDITION("!is", true)
	}
}