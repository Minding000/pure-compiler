package components.code_generation.llvm.models.general

import components.code_generation.llvm.models.declarations.ValueDeclaration
import components.code_generation.llvm.wrapper.LlvmBlock
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.general.HandleBlock

class HandleBlock(override val model: HandleBlock, val eventVariable: ValueDeclaration?, val block: StatementBlock):
	Unit(model, listOfNotNull(eventVariable, block)) {
	private lateinit var entryBlock: LlvmBlock

	override fun compile(constructor: LlvmConstructor) {
		val function = constructor.getParentFunction()
		entryBlock = constructor.createBlock(function, "handle_block_entry")
		constructor.select(entryBlock)
		val exceptionParameter = context.getExceptionParameter(constructor, function)
		if(eventVariable != null) {
			val exception = constructor.buildLoad(constructor.pointerType, exceptionParameter, "exception")
			eventVariable.compile(constructor)
			constructor.buildStore(exception, eventVariable.llvmLocation)
		}
		constructor.buildStore(constructor.nullPointer, exceptionParameter)
		block.compile(constructor)
	}

	fun jumpTo(constructor: LlvmConstructor) {
		constructor.buildJump(entryBlock)
	}
}
