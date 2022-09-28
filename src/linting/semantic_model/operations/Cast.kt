package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.QuantifiedType
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import parsing.syntax_tree.operations.Cast

class Cast(override val source: Cast, val value: Value, val variable: VariableValue?, val referenceType: Type,
		   val operator: Operator): Value(source) {

	init {
		units.add(value)
		if(variable != null)
			units.add(variable)
		type = if(operator.isConditional) {
			units.add(referenceType)
			ObjectType(source, Linter.Literals.BOOLEAN)
		} else if(operator == Operator.OPTIONAL_CAST) {
			val type = QuantifiedType(source, referenceType, hasDynamicQuantity = false, isOptional = true)
			units.add(type)
			type
		} else {
			units.add(referenceType)
			referenceType
		}
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, scope)
		if(operator.isConditional)
			linter.booleanLiteralScope?.let { literalScope -> type?.linkTypes(linter, literalScope) }
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		//TODO check if safe cast is possible
		//TODO check if optional, force or conditional cast is necessary
	}

	enum class Operator(val stringRepresentation: String, val isConditional: Boolean = false) {
		SAFE_CAST("as"),
		OPTIONAL_CAST("as?"),
		THROWING_CAST("as!"),
		CAST_CONDITION("is", true),
		NEGATED_CAST_CONDITION("is!", true)
	}
}