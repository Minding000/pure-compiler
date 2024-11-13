package components.semantic_model.control_flow

import components.code_generation.llvm.models.control_flow.NextStatement
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import logger.issues.loops.NextStatementOutsideOfLoop
import components.syntax_parser.syntax_tree.control_flow.NextStatement as NextStatementSyntaxTree

class NextStatement(override val source: NextStatementSyntaxTree, scope: Scope): SemanticModel(source, scope) {
	override val isInterruptingExecutionBasedOnStructure = true
	override val isInterruptingExecutionBasedOnStaticEvaluation = true
	var targetLoop: LoopStatement? = null

	override fun determineTypes() {
		super.determineTypes()
		determineTargetLoop()
	}

	private fun determineTargetLoop() {
		val surroundingLoop = scope.getSurroundingLoop()
		if(surroundingLoop == null) {
			context.addIssue(NextStatementOutsideOfLoop(source))
			return
		}
		targetLoop = surroundingLoop
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		tracker.registerNextStatement()
	}

	override fun toUnit() = NextStatement(this)
}
