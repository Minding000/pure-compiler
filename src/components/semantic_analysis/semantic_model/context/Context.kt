package components.semantic_analysis.semantic_model.context

import logger.Issue
import logger.Logger
import logger.Severity

class Context {
	val logger = Logger("compiler", Severity.INFO)
	val declarationStack = DeclarationStack(logger)

	fun addIssue(issue: Issue) = logger.add(issue)
}
