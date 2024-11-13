package components.code_generation.llvm.models.general

import components.code_generation.llvm.wrapper.LlvmBlock
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.types.ObjectType
import components.semantic_model.types.SelfType
import errors.internal.CompilerError
import java.util.*

class ErrorHandlingContext(override val model: ErrorHandlingContext, val mainBlock: StatementBlock,
						   val handleBlocks: List<HandleBlock> = emptyList(), val alwaysBlock: StatementBlock? = null):
	Unit(model, listOfNotNull(mainBlock, *handleBlocks.toTypedArray(), alwaysBlock)) {
	private var entryBlocks = HashMap<Unit, LlvmBlock>()
	private lateinit var exitBlock: LlvmBlock
	private var returnAddressVariable: LlvmValue? = null
	private val returnBlocks = LinkedList<LlvmBlock>()

	fun needsToBeCalled(): Boolean {
		return !(handleBlocks.isEmpty() && alwaysBlock == null)
	}

	override fun compile(constructor: LlvmConstructor) {
		var noReturnAddressBlock: LlvmBlock? = null
		if(alwaysBlock != null && !alwaysBlock.model.isInterruptingExecutionBasedOnStructure) {
			returnAddressVariable = constructor.buildStackAllocation(constructor.pointerType, "errorHandling_returnAddressVariable")
			noReturnAddressBlock = constructor.createBlock("noReturnAddress")
			constructor.buildStore(constructor.getBlockAddress(noReturnAddressBlock), returnAddressVariable)
			returnBlocks.add(noReturnAddressBlock)
		}
		exitBlock = constructor.createDetachedBlock("errorHandling_exit")
		val shouldAddExitBlock = handleBlocks.any { !it.model.isInterruptingExecutionBasedOnStructure } || alwaysBlock != null
		if(shouldAddExitBlock)
			constructor.addBlockToFunction(constructor.getParentFunction(), exitBlock)
		if(needsToBeCalled())
			compileErrorHandler(constructor, exitBlock)
		mainBlock.compile(constructor)
		if(!shouldAddExitBlock)
			return
		if(!mainBlock.model.isInterruptingExecutionBasedOnStructure)
			constructor.buildJump(exitBlock)
		constructor.select(exitBlock)
		if(alwaysBlock == null)
			return
		val exceptionParameter = context.getExceptionParameter(constructor)
		val exception = constructor.buildLoad(constructor.pointerType, exceptionParameter, "initialException")
		constructor.buildStore(constructor.nullPointer, exceptionParameter)
		alwaysBlock.compile(constructor)
		if(alwaysBlock.model.isInterruptingExecutionBasedOnStructure)
			return
		if(noReturnAddressBlock == null)
			throw CompilerError(model, "Block 'noReturnAddressBlock' is missing")
		val returnAddress = constructor.buildLoad(constructor.pointerType, returnAddressVariable, "returnAddress")
		constructor.buildJump(returnAddress, returnBlocks)
		constructor.select(noReturnAddressBlock)
		constructor.buildStore(exception, exceptionParameter)
		if(model.isInterruptingExecutionBasedOnStructure)
			context.handleException(constructor, model.parent)
		else
			context.continueRaise(constructor, model)
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
			if(!handleBlock.model.isInterruptingExecutionBasedOnStructure)
				constructor.buildJump(exitBlock)
			constructor.select(currentBlock)
			val referenceType = handleBlock.model.eventType
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
			context.handleException(constructor, model.parent)
		else
			constructor.buildJump(exitBlock)
		constructor.select(previousBlock)
	}

	fun jumpTo(constructor: LlvmConstructor, source: Unit) {
		val entryBlock = entryBlocks[source]
		if(entryBlock == null) {
			context.handleException(constructor, model.parent)
			return
		}
		constructor.buildJump(entryBlock)
	}

	fun runAlwaysBlock(constructor: LlvmConstructor, returnBlock: LlvmBlock = constructor.createBlock("return")) {
		if(alwaysBlock?.model?.isInterruptingExecutionBasedOnStructure == false) {
			constructor.buildStore(constructor.getBlockAddress(returnBlock), returnAddressVariable)
			returnBlocks.add(returnBlock)
		}
		constructor.buildJump(exitBlock)
		constructor.select(returnBlock)
	}
}
