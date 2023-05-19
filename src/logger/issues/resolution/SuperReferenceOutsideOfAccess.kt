package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class SuperReferenceOutsideOfAccess(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Super references are not allowed outside of member and index accesses."
	override val description = "Super references are not allowed outside of member and index accesses."
}
