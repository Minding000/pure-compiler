package logger.issues.declaration

import components.semantic_analysis.semantic_model.general.SemanticModel
import logger.Issue
import logger.Severity

class DuplicateChildModel(model: SemanticModel): Issue(Severity.WARNING, model.source) {
	override val text = "Model '$model' already exists in '${model.parent}'."
	override val description = "The semantic model should only be added once to avoid duplicate validation output and redundant work."
	override val isInternal = true
}
