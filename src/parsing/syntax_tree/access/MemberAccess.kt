package parsing.syntax_tree.access

import linting.Linter
import linting.semantic_model.operations.MemberAccess
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import parsing.syntax_tree.literals.Identifier
import util.indent

class MemberAccess(val target: ValueElement, val member: Identifier, private val isOptional: Boolean):
	ValueElement(target.start, member.end) {

	override fun concretize(linter: Linter, scope: MutableScope): MemberAccess {
		return MemberAccess(this, target.concretize(linter, scope), member.concretize(linter, scope), isOptional)
	}

	override fun toString(): String {
		return "MemberAccess {${"\n$target${if(isOptional) "?." else "."}$member".indent()}\n}"
	}
}
