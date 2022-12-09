package components.semantic_analysis.semantic_model.definitions

import components.syntax_parser.syntax_tree.general.Element

interface MemberDeclaration {
	val source: Element
	var parentDefinition: TypeDefinition
	var signatureString: String
	val isAbstract: Boolean

	fun canBeOverriddenBy(other: MemberDeclaration?): Boolean {
		if(other == null)
			return false
		if(other.signatureString != signatureString)
			return false
		return true
	}
}
