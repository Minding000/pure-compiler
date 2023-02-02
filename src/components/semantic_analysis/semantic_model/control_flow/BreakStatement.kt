package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import messages.Message
import components.syntax_parser.syntax_tree.control_flow.BreakStatement as BreakStatementSyntaxTree

class BreakStatement(override val source: BreakStatementSyntaxTree): Unit(source) {
	var targetLoop: LoopStatement? = null
	override val isInterruptingExecution = true

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		val surroundingLoop = scope.getSurroundingLoop()
		if(surroundingLoop == null) {
			linter.addMessage(source, "Break statements are not allowed outside of loops.", Message.Type.ERROR)
		} else {
			surroundingLoop.mightGetBrokenOutOf = true
			targetLoop = surroundingLoop
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		tracker.registerBreakStatement()
	}
}
