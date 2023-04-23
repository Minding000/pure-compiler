package components.semantic_analysis.semantic_model.definitions

import components.syntax_parser.syntax_tree.general.Element

interface MemberDeclaration {
	val source: Element
	val parentDefinition: TypeDefinition?
	val memberIdentifier: String
	val isAbstract: Boolean

	fun canBeOverriddenBy(other: MemberDeclaration?): Boolean {
		if(other == null)
			return false
		return other.memberIdentifier == memberIdentifier
	}
}
