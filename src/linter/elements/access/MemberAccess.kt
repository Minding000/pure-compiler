package linter.elements.access

import linter.elements.general.Unit
import parsing.ast.access.MemberAccess

class MemberAccess(val source: MemberAccess, val target: Unit, val member: Unit, val isOptional: Boolean): Unit() {

	init {
		units.add(target)
		units.add(member)
	}
}