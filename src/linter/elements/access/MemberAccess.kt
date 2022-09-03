package linter.elements.access

import linter.Linter
import linter.elements.literals.QuantifiedType
import linter.elements.values.Value
import linter.elements.values.VariableValue
import linter.scopes.Scope
import parsing.ast.access.MemberAccess

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