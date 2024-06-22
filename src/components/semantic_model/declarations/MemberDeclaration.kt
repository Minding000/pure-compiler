package components.semantic_model.declarations

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

interface MemberDeclaration {
	val source: SyntaxTreeNode
	val parentTypeDeclaration: TypeDeclaration?
	/** The identifier used to resolve the member at runtime. Might not represent the signature accurately. */
	val memberIdentifier: String //TODO don't use this to describe the member (like in issues)
	val isAbstract: Boolean
}
