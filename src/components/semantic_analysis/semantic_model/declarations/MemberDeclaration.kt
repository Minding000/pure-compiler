package components.semantic_analysis.semantic_model.declarations

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

interface MemberDeclaration {
	val source: SyntaxTreeNode
	val parentTypeDeclaration: TypeDeclaration?
	val memberIdentifier: String //TODO This should not be used as an identifier. The declaration object identity or a specialized check should be used instead
	val isAbstract: Boolean

	//TODO This should use 'fulfillsInheritanceRequirements()' instead
	fun canBeOverriddenBy(other: MemberDeclaration?): Boolean {
		if(other == null)
			return false
		return other.memberIdentifier == memberIdentifier
	}
}
