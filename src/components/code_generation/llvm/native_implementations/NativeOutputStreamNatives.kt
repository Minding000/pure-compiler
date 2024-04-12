package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.NativeRegistry

class NativeOutputStreamNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("NativeOutputStream.writeByte(Byte)", ::writeByte)
		registry.registerNativeImplementation("NativeOutputStream.writeBytes(<Byte>Array)", ::writeBytes)
	}

	//TODO implement
	private fun writeByte(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor)
		val thisObject = context.getThisParameter(constructor)

		constructor.buildReturn()
	}

	private fun writeBytes(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor) //TODO error handling
		val thisObject = context.getThisParameter(constructor)
		val arrayObject = constructor.getLastParameter()
		val identifierProperty = context.resolveMember(constructor, thisObject, "identifier")
		val identifier = constructor.buildLoad(constructor.i32Type, identifierProperty, "identifier")
		val mode = constructor.buildGlobalAsciiCharArray("NativeOutputStream_writeMode", "a")
		val fileDescriptor = constructor.buildFunctionCall(context.llvmOpenFunctionType, context.llvmOpenFunction, listOf(identifier, mode), "fileDescriptor")

		val arraySizeProperty = context.resolveMember(constructor, arrayObject, "size")
		val arraySize = constructor.buildLoad(constructor.i32Type, arraySizeProperty, "size")
		val arraySizeAsLong = constructor.buildCastFromIntegerToLong(arraySize, "sizeAsLong")

		val arrayValueProperty = constructor.buildGetPropertyPointer(context.arrayDeclarationType, arrayObject, context.arrayValueIndex, "arrayValueProperty")
		val arrayValue = constructor.buildLoad(constructor.pointerType, arrayValueProperty, "arrayValue")
		val byteSize = constructor.buildInt64(1)
		constructor.buildFunctionCall(context.llvmWriteFunctionType, context.llvmWriteFunction, listOf(arrayValue, byteSize, arraySizeAsLong, fileDescriptor))

		constructor.buildReturn()
	}
}
