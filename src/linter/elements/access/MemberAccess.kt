package linter.elements.access

import linter.Linter
import linter.elements.general.Unit
import linter.elements.values.Value
import linter.scopes.Scope
import parsing.ast.access.MemberAccess

class MemberAccess(override val source: MemberAccess, val target: Value, val member: Value, val isOptional: Boolean): Value(source) {

	init {
		units.add(target)
		units.add(member)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		target.linkTypes(linter, scope)
		target.type?.let {
			member.linkTypes(linter, it.scope)
		}
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		target.linkReferences(linter, scope)
		target.type?.let {
			member.linkReferences(linter, it.scope)
			type = member.type
		}
	}
}