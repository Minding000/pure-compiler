package components.syntax_parser.syntax_tree.access

import components.linting.Linter
import components.linting.semantic_model.operations.MemberAccess as SemanticMemberAccessModel
import components.linting.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.Identifier
import util.indent

class MemberAccess(val target: ValueElement, val member: Identifier, private val isOptional: Boolean):
	ValueElement(target.start, member.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticMemberAccessModel {
		return SemanticMemberAccessModel(this, target.concretize(linter, scope),
			member.concretize(linter, scope), isOptional)
	}

	override fun toString(): String {
		return "MemberAccess {${"\n$target${if(isOptional) "?." else "."}$member".indent()}\n}"
	}
}
