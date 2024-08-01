package components.semantic_model.general

import components.code_generation.llvm.LlvmBlock
import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
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

class ErrorHandlingContext(override val source: SyntaxTreeNode, scope: Scope, val mainBlock: StatementBlock,
						   val handleBlocks: List<HandleBlock> = emptyList(), val alwaysBlock: StatementBlock? = null):
	SemanticModel(source, scope) {
	override var isInterruptingExecutionBasedOnStructure = false
	override var isInterruptingExecutionBasedOnStaticEvaluation = false
	private var entryBlocks = HashMap<SemanticModel, LlvmBlock>()
	private lateinit var exitBlock: LlvmBlock
	private var returnAddressVariable: LlvmValue? = null
	private val returnBlocks = LinkedList<LlvmBlock>()

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
		var noReturnAddressBlock: LlvmBlock? = null
		if(alwaysBlock != null && !alwaysBlock.isInterruptingExecutionBasedOnStructure) {
			returnAddressVariable = constructor.buildStackAllocation(constructor.pointerType, "errorHandling_returnAddressVariable")
			noReturnAddressBlock = constructor.createBlock("noReturnAddress")
			constructor.buildStore(constructor.getBlockAddress(noReturnAddressBlock), returnAddressVariable)
			returnBlocks.add(noReturnAddressBlock)
		}
		exitBlock = constructor.createDetachedBlock("errorHandling_exit")
		val shouldAddExitBlock = handleBlocks.any { !it.isInterruptingExecutionBasedOnStructure } || alwaysBlock != null
		if(shouldAddExitBlock)
			constructor.addBlockToFunction(constructor.getParentFunction(), exitBlock)
		if(needsToBeCalled())
			compileErrorHandler(constructor, exitBlock)
		mainBlock.compile(constructor)
		if(!shouldAddExitBlock)
			return
		if(!mainBlock.isInterruptingExecutionBasedOnStructure)
			constructor.buildJump(exitBlock)
		constructor.select(exitBlock)
		if(alwaysBlock == null)
			return
		val exceptionParameter = context.getExceptionParameter(constructor)
		val exception = constructor.buildLoad(constructor.pointerType, exceptionParameter, "initialException")
		constructor.buildStore(constructor.nullPointer, exceptionParameter)
		alwaysBlock.compile(constructor)
		if(alwaysBlock.isInterruptingExecutionBasedOnStructure)
			return
		if(noReturnAddressBlock == null)
			throw CompilerError(source, "Block 'noReturnAddressBlock' is missing")
		val returnAddress = constructor.buildLoad(constructor.pointerType, returnAddressVariable, "returnAddress")
		constructor.buildJump(returnAddress, returnBlocks)
		constructor.select(noReturnAddressBlock)
		constructor.buildStore(exception, exceptionParameter)
		if(isInterruptingExecutionBasedOnStructure)
			context.handleException(constructor, parent)
		else
			context.continueRaise(constructor, this)
	}

	private fun compileErrorHandler(constructor: LlvmConstructor, exitBlock: LlvmBlock) {
		val previousBlock = constructor.getCurrentBlock()
		val function = constructor.getParentFunction(previousBlock)
		val entryBlock = constructor.createBlock(function, "errorHandling_entry")
		constructor.select(entryBlock)
		val exceptionParameter = context.getExceptionParameter(constructor, function)
		val exception = constructor.buildLoad(constructor.pointerType, exceptionParameter, "exception")
		val exceptionClass = context.getClassDefinition(constructor, exception)
		var currentBlock = entryBlock
		entryBlocks[mainBlock] = entryBlock
		for(handleBlock in handleBlocks) {
			val matchBlock = constructor.createBlock(function, "errorHandling_match")
			val noMatchBlock = constructor.createBlock(function, "errorHandling_noMatch")
			entryBlocks[handleBlock] = noMatchBlock
			handleBlock.compile(constructor)
			if(!handleBlock.isInterruptingExecutionBasedOnStructure)
				constructor.buildJump(exitBlock)
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
			constructor.buildJump(matchesErrorType, matchBlock, noMatchBlock)
			constructor.select(matchBlock)
			handleBlock.jumpTo(constructor)
			currentBlock = noMatchBlock
		}
		constructor.select(currentBlock)
		if(alwaysBlock == null)
			context.handleException(constructor, parent)
		else
			constructor.buildJump(exitBlock)
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

	fun runAlwaysBlock(constructor: LlvmConstructor, returnBlock: LlvmBlock = constructor.createBlock("return")) {
		if(alwaysBlock?.isInterruptingExecutionBasedOnStructure == false) {
			constructor.buildStore(constructor.getBlockAddress(returnBlock), returnAddressVariable)
			returnBlocks.add(returnBlock)
		}
		constructor.buildJump(exitBlock)
		constructor.select(returnBlock)
	}
}
