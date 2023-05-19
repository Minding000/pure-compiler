package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import logger.issues.loops.NextStatementOutsideOfLoop
import components.syntax_parser.syntax_tree.control_flow.NextStatement as NextStatementSyntaxTree

class NextStatement(override val source: NextStatementSyntaxTree, scope: Scope): Unit(source, scope) {
	private var targetLoop: LoopStatement? = null
	override val isInterruptingExecution = true

	override fun determineTypes() {
		super.determineTypes()
		val surroundingLoop = scope.getSurroundingLoop()
		if(surroundingLoop == null)
			context.addIssue(NextStatementOutsideOfLoop(source))
		else
			targetLoop = surroundingLoop
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		tracker.registerNextStatement()
	}
}
