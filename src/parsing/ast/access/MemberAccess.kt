package parsing.ast.access

import linter.Linter
import linter.elements.access.MemberAccess
import linter.scopes.MutableScope
import parsing.ast.general.ValueElement
import util.indent

class MemberAccess(val target: ValueElement, val member: ValueElement, private val isOptional: Boolean):
	ValueElement(target.start, member.end) {

	override fun concretize(linter: Linter, scope: MutableScope): MemberAccess {
		return MemberAccess(this, target.concretize(linter, scope), member.concretize(linter, scope), isOptional)
	}

	override fun toString(): String {
		return "MemberAccess {${"\n$target${if(isOptional) "?." else "."}$member".indent()}\n}"
	}
}