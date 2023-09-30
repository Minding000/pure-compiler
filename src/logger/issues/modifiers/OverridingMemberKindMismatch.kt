package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class OverridingMemberKindMismatch(source: SyntaxTreeNode, name: String, memberKind: String, superMemberKind: String):
	Issue(Severity.ERROR, source) {
	override val text = "'$name' $memberKind cannot override '$name' $superMemberKind."
	override val description = "A member with the same but of a different kind exists in the super type."
	override val suggestion = "Choose a different name."
}
