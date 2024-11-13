package components.semantic_model.control_flow

import components.code_generation.llvm.models.control_flow.BreakStatement
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import errors.internal.CompilerError
import logger.issues.loops.BreakStatementOutsideOfLoop
import components.syntax_parser.syntax_tree.control_flow.BreakStatement as BreakStatementSyntaxTree

class BreakStatement(override val source: BreakStatementSyntaxTree, scope: Scope): SemanticModel(source, scope) {
	var targetLoop: LoopStatement? = null
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

	override fun toUnit() = BreakStatement(this)

	override fun compile(constructor: LlvmConstructor) {
		val targetLoop = targetLoop ?: throw CompilerError(source, "Break statement outside of loop.")
		val errorHandlingContext = scope.getSurroundingAlwaysBlock()
		if(errorHandlingContext?.isIn(targetLoop) == true)
			errorHandlingContext.runAlwaysBlock(constructor)
		targetLoop.jumpOut(constructor)
	}
}
