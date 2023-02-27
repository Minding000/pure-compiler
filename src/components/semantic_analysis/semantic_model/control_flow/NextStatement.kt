package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import messages.Message
import components.syntax_parser.syntax_tree.control_flow.NextStatement as NextStatementSyntaxTree

class NextStatement(override val source: NextStatementSyntaxTree, scope: Scope): Unit(source, scope) {
	private var targetLoop: LoopStatement? = null
	override val isInterruptingExecution = true

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		val surroundingLoop = scope.getSurroundingLoop()
		if(surroundingLoop == null) {
			linter.addMessage(source, "Next statements are not allowed outside of loops.", Message.Type.ERROR)
		} else {
			targetLoop = surroundingLoop
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		tracker.registerNextStatement()
	}
}
