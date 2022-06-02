package linter.elements.operations

import linter.elements.literals.Type
import linter.elements.values.Value
import linter.elements.values.VariableValue
import parsing.ast.operations.Cast

class Cast(val source: Cast, val value: Value, val variable: VariableValue?, type: Type, val operator: Operator):
	Value() {

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