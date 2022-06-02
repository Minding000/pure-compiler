package parsing.ast.access

import linter.Linter
import linter.elements.access.MemberAccess
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.general.ValueElement
import util.indent

class MemberAccess(val target: Element, val member: Element, private val isOptional: Boolean): ValueElement(target.start, member.end) {

	override fun concretize(linter: Linter, scope: Scope): MemberAccess {
		return MemberAccess(this, target.concretize(linter, scope), member.concretize(linter, scope), isOptional)
	}

	override fun toString(): String {
		return "MemberAccess {${"\n$target${if(isOptional) "?." else "."}$member".indent()}\n}"
	}
}