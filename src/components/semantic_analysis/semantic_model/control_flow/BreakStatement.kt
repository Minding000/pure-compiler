package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import logger.issues.loops.BreakStatementOutsideOfLoop
import components.syntax_parser.syntax_tree.control_flow.BreakStatement as BreakStatementSyntaxTree

class BreakStatement(override val source: BreakStatementSyntaxTree, scope: Scope): Unit(source, scope) {
	var targetLoop: LoopStatement? = null
	override val isInterruptingExecution = true

	override fun determineTypes(linter: Linter) {
		super.determineTypes(linter)
		val surroundingLoop = scope.getSurroundingLoop()
		if(surroundingLoop == null) {
			linter.addIssue(BreakStatementOutsideOfLoop(source))
		} else {
			surroundingLoop.mightGetBrokenOutOf = true
			targetLoop = surroundingLoop
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		tracker.registerBreakStatement()
	}
}
