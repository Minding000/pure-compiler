package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.StatementSection

class ErrorHandlingContext(override val source: StatementSection, val mainBlock: StatementBlock,
						   val handleBlocks: List<HandleBlock>, val alwaysBlock: StatementBlock?): Unit(source) {

	init {
		addUnits(mainBlock, alwaysBlock)
		addUnits(handleBlocks)
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		val initialState = tracker.currentState.copy()
		// Analyse main block
		tracker.currentState.firstVariableUsages.clear()
		mainBlock.analyseDataFlow(linter, tracker)
		// Collect usages that should link to the handle blocks
		val potentiallyLastVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
		if(handleBlocks.isNotEmpty() || alwaysBlock != null)
			tracker.collectAllUsagesInto(potentiallyLastVariableUsages)
		if(handleBlocks.isNotEmpty()) {
			// Analyse handle blocks
			val mainBlockState = tracker.currentState.copy()
			for(handleBlock in handleBlocks) {
				tracker.setVariableStates(initialState)
				tracker.currentState.firstVariableUsages.clear()
				handleBlock.analyseDataFlow(linter, tracker)
				tracker.linkToStartFrom(potentiallyLastVariableUsages)
				tracker.collectAllUsagesInto(potentiallyLastVariableUsages)
			}
			tracker.setVariableStates(mainBlockState)
		}
		// Analyse always block (if it exists)
		if(alwaysBlock != null) {
			// First analyse for complete execution
			alwaysBlock.analyseDataFlow(linter, tracker)
			val completeExecutionState = tracker.currentState.copy()
			// Then analyse for failure case
			tracker.setVariableStates(initialState)
			tracker.currentState.firstVariableUsages.clear()
			alwaysBlock.analyseDataFlow(linter, tracker)
			tracker.markAllUsagesAsExiting()
			tracker.linkToStartFrom(potentiallyLastVariableUsages)
			tracker.setVariableStates(completeExecutionState)
		}
	}
}
