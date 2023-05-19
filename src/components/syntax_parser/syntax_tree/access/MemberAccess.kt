package components.syntax_parser.syntax_tree.access

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import util.indent
import components.semantic_analysis.semantic_model.operations.MemberAccess as SemanticMemberAccessModel

class MemberAccess(val target: ValueElement, val member: ValueElement, private val isOptional: Boolean):
	ValueElement(target.start, member.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticMemberAccessModel {
		return SemanticMemberAccessModel(this, scope, target.toSemanticModel(scope), member.toSemanticModel(scope), isOptional)
	}

	override fun toString(): String {
		return "MemberAccess {${"\n$target${if(isOptional) "?." else "."}$member".indent()}\n}"
	}
}
