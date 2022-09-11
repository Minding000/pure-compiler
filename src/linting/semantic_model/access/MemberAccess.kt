package linting.semantic_model.access

import linting.Linter
import linting.semantic_model.literals.QuantifiedType
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.access.MemberAccess

class MemberAccess(override val source: MemberAccess, val target: Value, val member: VariableValue,
				   private val isOptional: Boolean): Value(source) {

	init {
		units.add(target)
		units.add(member)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		target.linkValues(linter, scope)
		target.type?.let {
			member.linkValues(linter, it.scope)
			member.type?.let { baseType ->
				type = if(isOptional && !(baseType is QuantifiedType && baseType.isOptional))
					QuantifiedType(source, baseType, hasDynamicQuantity = false, isOptional = true)
				else
					baseType
			}
		}
	}
}