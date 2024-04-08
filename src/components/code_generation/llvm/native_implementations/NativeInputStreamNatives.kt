package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.NativeRegistry

object NativeInputStreamNatives {
	lateinit var context: Context

	fun load(registry: NativeRegistry) {
		context = registry.context
		registry.registerNativeImplementation("NativeInputStream.readByte(): Byte", ::readByte)
		registry.registerNativeImplementation("NativeInputStream.readBytes(): <ByteArray>", ::readBytes)
	}

	//TODO implement
	fun readByte(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor)
		val thisInt = context.getThisParameter(constructor)

		constructor.buildReturn(thisInt)
	}

	//TODO implement
	fun readBytes(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor)
		val thisInt = context.getThisParameter(constructor)
		//TODO
		// - choose buffer size
		// - create buffer
		// - check result -> loop
		constructor.buildFunctionCall(context.llvmReadFunctionType, context.llvmReadFunction, listOf(constructor.nullPointer))
		constructor.buildReturn(thisInt)
	}
}
