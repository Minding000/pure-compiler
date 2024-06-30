package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import logger.issues.loops.BreakStatementOutsideOfLoop
import components.syntax_parser.syntax_tree.control_flow.BreakStatement as BreakStatementSyntaxTree

class BreakStatement(override val source: BreakStatementSyntaxTree, scope: Scope): SemanticModel(source, scope) {
	private var targetLoop: LoopStatement? = null
	override val isInterruptingExecutionBasedOnStructure = true
	override val isInterruptingExecutionBasedOnStaticEvaluation = true

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

	override fun compile(constructor: LlvmConstructor) {
		val errorHandlingContext = scope.getSurroundingAlwaysBlock()
		errorHandlingContext?.runAlwaysBlock(constructor)
		targetLoop?.jumpOut(constructor)
	}
}
