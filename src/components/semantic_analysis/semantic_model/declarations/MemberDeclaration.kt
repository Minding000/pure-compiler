package components.semantic_analysis.semantic_model.declarations

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

interface MemberDeclaration {
	val source: SyntaxTreeNode
	val parentTypeDeclaration: TypeDeclaration?
	val memberIdentifier: String //TODO This should not be used as an identifier. The declaration object identity or a specialized check should be used instead
	val isAbstract: Boolean
}
