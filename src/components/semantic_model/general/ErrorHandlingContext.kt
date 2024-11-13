package components.semantic_model.general

import components.code_generation.llvm.models.general.ErrorHandlingContext
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import java.util.*

class ErrorHandlingContext(override val source: SyntaxTreeNode, scope: Scope, val mainBlock: StatementBlock,
						   val handleBlocks: List<HandleBlock> = emptyList(), val alwaysBlock: StatementBlock? = null):
	SemanticModel(source, scope) {
	override var isInterruptingExecutionBasedOnStructure = false
	override var isInterruptingExecutionBasedOnStaticEvaluation = false
	lateinit var unit: ErrorHandlingContext

	init {
		addSemanticModels(mainBlock, alwaysBlock)
		addSemanticModels(handleBlocks)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val initialState = tracker.currentState.copy()
		// Analyse main block
		val mainBlockReferencePoint = tracker.currentState.createReferencePoint()
		mainBlock.analyseDataFlow(tracker)
		// Collect usages that should link to the handle blocks
		// This is done to avoid creating a state for each variable usage in an error handling context
		val potentiallyLastVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
		if(handleBlocks.isNotEmpty() || alwaysBlock != null)
			tracker.collectAllUsagesInto(mainBlockReferencePoint, potentiallyLastVariableUsages)
		tracker.currentState.removeReferencePoint(mainBlockReferencePoint)
		if(handleBlocks.isNotEmpty()) {
			// Analyse handle blocks
			val mainBlockState = tracker.currentState.copy()
			val handleBlockStates = LinkedList<VariableTracker.VariableState>()
			for(handleBlock in handleBlocks) {
				tracker.setVariableStates(initialState)
				tracker.addLastVariableUsages(potentiallyLastVariableUsages)
				val handleBlockReferencePoint = tracker.currentState.createReferencePoint()
				handleBlock.analyseDataFlow(tracker)
				tracker.collectAllUsagesInto(handleBlockReferencePoint, potentiallyLastVariableUsages)
				tracker.currentState.removeReferencePoint(handleBlockReferencePoint)
				handleBlockStates.add(tracker.currentState.copy())
			}
			tracker.setVariableStates(mainBlockState, *handleBlockStates.toTypedArray())
		}
		// Analyse always block (if it exists)
		if(alwaysBlock != null) {
			// First analyse for complete execution
			alwaysBlock.analyseDataFlow(tracker)
			val completeExecutionState = tracker.currentState.copy()
			// Then analyse for failure case
			tracker.setVariableStates(initialState)
			tracker.addLastVariableUsages(potentiallyLastVariableUsages)
			val alwaysBlockReferencePoint = tracker.currentState.createReferencePoint()
			alwaysBlock.analyseDataFlow(tracker)
			tracker.markAllUsagesAsExiting(alwaysBlockReferencePoint)
			tracker.currentState.removeReferencePoint(alwaysBlockReferencePoint)
			tracker.setVariableStates(completeExecutionState)
		}
		evaluateExecutionFlow()
	}

	private fun evaluateExecutionFlow() {
		isInterruptingExecutionBasedOnStructure = mainBlock.isInterruptingExecutionBasedOnStructure
		isInterruptingExecutionBasedOnStaticEvaluation = mainBlock.isInterruptingExecutionBasedOnStaticEvaluation
		if(isInterruptingExecutionBasedOnStructure)
			isInterruptingExecutionBasedOnStructure = handleBlocks.all(SemanticModel::isInterruptingExecutionBasedOnStructure)
		if(isInterruptingExecutionBasedOnStaticEvaluation)
			isInterruptingExecutionBasedOnStaticEvaluation = handleBlocks.all(SemanticModel::isInterruptingExecutionBasedOnStaticEvaluation)
		if(alwaysBlock != null) {
			if(alwaysBlock.isInterruptingExecutionBasedOnStructure)
				isInterruptingExecutionBasedOnStructure = true
			if(alwaysBlock.isInterruptingExecutionBasedOnStaticEvaluation)
				isInterruptingExecutionBasedOnStaticEvaluation = true
		}
	}

	fun getLastStatement(): SemanticModel? {
		return mainBlock.statements.lastOrNull()
	}

	fun getValue(): Value? {
		return getLastStatement() as? Value
	}

	fun needsToBeCalled(): Boolean {
		return !(handleBlocks.isEmpty() && alwaysBlock == null)
	}

	override fun toUnit(): ErrorHandlingContext {
		val unit = ErrorHandlingContext(this, mainBlock.toUnit(), handleBlocks.map(HandleBlock::toUnit), alwaysBlock?.toUnit())
		this.unit = unit
		return unit
	}
}
