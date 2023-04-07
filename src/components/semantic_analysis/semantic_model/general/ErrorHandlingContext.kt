package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.StatementSection
import java.util.*

class ErrorHandlingContext(override val source: StatementSection, scope: Scope, val mainBlock: StatementBlock,
						   val handleBlocks: List<HandleBlock>, val alwaysBlock: StatementBlock?): Unit(source, scope) {

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
		val potentiallyLastVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>() // This is done to avoid creating a state for each variable usage in an error handling context
		if(handleBlocks.isNotEmpty() || alwaysBlock != null)
			tracker.collectAllUsagesInto(potentiallyLastVariableUsages)
		if(handleBlocks.isNotEmpty()) {
			// Analyse handle blocks
			val mainBlockState = tracker.currentState.copy()
			val handleBlockStates = LinkedList<VariableTracker.VariableState>()
			for(handleBlock in handleBlocks) {
				tracker.setVariableStates(initialState)
				tracker.addLastVariableUsages(potentiallyLastVariableUsages)
				tracker.currentState.firstVariableUsages.clear()
				handleBlock.analyseDataFlow(linter, tracker)
				handleBlockStates.add(tracker.currentState.copy())
				tracker.collectAllUsagesInto(potentiallyLastVariableUsages)
			}
			tracker.setVariableStates(mainBlockState, *handleBlockStates.toTypedArray())
		}
		// Analyse always block (if it exists)
		if(alwaysBlock != null) {
			// First analyse for complete execution
			alwaysBlock.analyseDataFlow(linter, tracker)
			val completeExecutionState = tracker.currentState.copy()
			// Then analyse for failure case
			tracker.setVariableStates(initialState)
			tracker.addLastVariableUsages(potentiallyLastVariableUsages)
			tracker.currentState.firstVariableUsages.clear()
			alwaysBlock.analyseDataFlow(linter, tracker)
			tracker.markAllUsagesAsExiting()
			tracker.setVariableStates(completeExecutionState)
		}
	}
}
