package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import logger.issues.loops.BreakStatementOutsideOfLoop
import components.syntax_parser.syntax_tree.control_flow.BreakStatement as BreakStatementSyntaxTree

class BreakStatement(override val source: BreakStatementSyntaxTree, scope: Scope): SemanticModel(source, scope) {
	var targetLoop: LoopStatement? = null
	override val isInterruptingExecution = true

	override fun determineTypes() {
		super.determineTypes()
		val surroundingLoop = scope.getSurroundingLoop()
		if(surroundingLoop == null) {
			context.addIssue(BreakStatementOutsideOfLoop(source))
		} else {
			surroundingLoop.mightGetBrokenOutOf = true
			targetLoop = surroundingLoop
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		tracker.registerBreakStatement()
	}
}
