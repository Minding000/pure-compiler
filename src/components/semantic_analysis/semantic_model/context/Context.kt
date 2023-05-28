package components.semantic_analysis.semantic_model.context

import logger.Issue
import logger.Logger

class Context {
	val logger = Logger("compiler")
	val declarationStack = DeclarationStack(logger)

	fun addIssue(issue: Issue) = logger.add(issue)
}
