package components.semantic_model.general

import components.code_generation.llvm.LlvmBlock
import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.SelfType
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import java.util.*

//TODO handle LLVM side of always block
class ErrorHandlingContext(override val source: SyntaxTreeNode, scope: Scope, val mainBlock: StatementBlock,
						   val handleBlocks: List<HandleBlock> = emptyList(), val alwaysBlock: StatementBlock? = null):
	SemanticModel(source, scope) {
	override var isInterruptingExecutionBasedOnStructure = false
	override var isInterruptingExecutionBasedOnStaticEvaluation = false
	private var entryBlocks = HashMap<SemanticModel, LlvmBlock>()

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

	override fun compile(constructor: LlvmConstructor) {
		val exitBlock = constructor.createDetachedBlock("error_handling_exit")
		val shouldAddExitBlock = !(isInterruptingExecutionBasedOnStructure || (handleBlocks.isEmpty() && alwaysBlock == null))
		if(shouldAddExitBlock)
			constructor.addBlockToFunction(constructor.getParentFunction(), exitBlock)
		if(needsToBeCalled())
			compileErrorHandler(constructor, exitBlock)
		mainBlock.compile(constructor)
		if(shouldAddExitBlock) {
			if(!mainBlock.isInterruptingExecutionBasedOnStructure)
				constructor.buildJump(exitBlock)
			constructor.select(exitBlock)
			if(alwaysBlock != null) {
				val exceptionParameter = context.getExceptionParameter(constructor)
				val exception = constructor.buildLoad(constructor.pointerType, exceptionParameter, "initialException")
				constructor.buildStore(constructor.nullPointer, exceptionParameter)
				alwaysBlock.compile(constructor)
				constructor.buildStore(exception, exceptionParameter)
				context.continueRaise(constructor, parent)
			}
		}
	}

	private fun compileErrorHandler(constructor: LlvmConstructor, exitBlock: LlvmBlock) {
		val previousBlock = constructor.getCurrentBlock()
		val function = constructor.getParentFunction(previousBlock)
		val entryBlock = constructor.createBlock(function, "error_handling_entry")
		constructor.select(entryBlock)
		val exceptionParameter = context.getExceptionParameter(constructor, function)
		val exception = constructor.buildLoad(constructor.pointerType, exceptionParameter, "exception")
		val exceptionClass = context.getClassDefinition(constructor, exception)
		var currentBlock = entryBlock
		entryBlocks[mainBlock] = entryBlock
		for(handleBlock in handleBlocks) {
			handleBlock.compile(constructor)
			if(!handleBlock.isInterruptingExecutionBasedOnStructure)
				constructor.buildJump(exitBlock) //TODO or to always block if exits
			constructor.select(currentBlock)
			val referenceType = handleBlock.eventType
			val referenceTypeDeclaration = when(referenceType) {
				is ObjectType -> referenceType.getTypeDeclaration()
				is SelfType -> referenceType.typeDeclaration
				else -> throw CompilerError(referenceType.source,
					"Handle blocks do not support complex types at the moment. Provided type: $referenceType")
			}
			val referenceClassDefinition = referenceTypeDeclaration?.llvmClassDefinition
				?: throw CompilerError(referenceType.source, "Missing class definition for type '$referenceType'.")
			val matchesErrorType = constructor.buildPointerEqualTo(exceptionClass, referenceClassDefinition, "matchesErrorType")
			val matchBlock = constructor.createBlock(function, "error_handling_match")
			val noMatchBlock = constructor.createBlock(function, "error_handling_no_match")
			constructor.buildJump(matchesErrorType, matchBlock, noMatchBlock)
			constructor.select(matchBlock)
			handleBlock.jumpTo(constructor)
			currentBlock = noMatchBlock
			entryBlocks[handleBlock] = noMatchBlock
		}
		constructor.select(currentBlock)
		context.handleException(constructor, parent)
		constructor.select(previousBlock)
	}

	fun jumpTo(constructor: LlvmConstructor, source: SemanticModel) {
		val entryBlock = entryBlocks[source]
		if(entryBlock == null) {
			context.handleException(constructor, parent)
			return
		}
		constructor.buildJump(entryBlock)
	}
}
