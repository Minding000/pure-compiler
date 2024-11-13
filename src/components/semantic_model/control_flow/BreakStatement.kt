package components.semantic_model.control_flow

import components.code_generation.llvm.models.control_flow.BreakStatement
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import logger.issues.loops.BreakStatementOutsideOfLoop
import components.syntax_parser.syntax_tree.control_flow.BreakStatement as BreakStatementSyntaxTree

class BreakStatement(override val source: BreakStatementSyntaxTree, scope: Scope): SemanticModel(source, scope) {
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
			context.addIssue(BreakStatementOutsideOfLoop(source))
			return
		}
		surroundingLoop.mightGetBrokenOutOf = true
		targetLoop = surroundingLoop
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		tracker.registerBreakStatement()
	}

	override fun toUnit() = BreakStatement(this)
}
