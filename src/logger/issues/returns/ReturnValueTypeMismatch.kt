package logger.issues.returns

import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ReturnValueTypeMismatch(source: SyntaxTreeNode, returnedType: Type, returnType: Type): Issue(Severity.ERROR, source) {
	override val text = "The type '$returnedType' of the returned value doesn't match the declared return type '$returnType'."
	override val description = "The referenced callable defines a return type, but the returned value is of a different type."
}
