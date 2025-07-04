package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.general.SemanticModel

class NativeOutputStreamNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("NativeOutputStream.writeByte(Byte)", ::writeByte)
		registry.registerNativeImplementation("NativeOutputStream.writeBytes(ByteArray)", ::writeBytes)
		registry.registerNativeImplementation("NativeOutputStream.flush()", ::flush)
	}

	private fun writeByte(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor) //TODO error handling
		val thisObject = context.getThisParameter(constructor)
		val byte = constructor.getLastParameter()

		val handleProperty = context.standardLibrary.nativeOutputStream.getNativeValueProperty(constructor, thisObject)
		val handle = constructor.buildLoad(constructor.pointerType, handleProperty, "handle")

		val byteVariable = constructor.buildStackAllocation(constructor.byteType, "byteVariable", byte)
		val byteSize = constructor.buildInt64(1)
		val byteCount = constructor.buildInt64(1)
		constructor.buildFunctionCall(context.externalFunctions.streamWrite, listOf(byteVariable, byteSize, byteCount, handle))

		constructor.buildReturn()
	}

	private fun writeBytes(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor) //TODO error handling
		val thisObject = context.getThisParameter(constructor)
		val arrayObject = constructor.getLastParameter()

		val handleProperty = context.standardLibrary.nativeOutputStream.getNativeValueProperty(constructor, thisObject)
		val handle = constructor.buildLoad(constructor.pointerType, handleProperty, "handle")

		val arraySizeProperty = context.resolveMember(constructor, arrayObject, "size")
		val arraySize = constructor.buildLoad(constructor.i32Type, arraySizeProperty, "size")
		val arraySizeAsLong = constructor.buildCastFromIntegerToLong(arraySize, "sizeAsLong")

		val byteArrayRuntimeClass = context.standardLibrary.byteArray
		val arrayValueProperty = byteArrayRuntimeClass.getNativeValueProperty(constructor, arrayObject)
		val arrayValue = constructor.buildLoad(constructor.pointerType, arrayValueProperty, "arrayValue")
		val byteSize = constructor.buildInt64(1)
		constructor.buildFunctionCall(context.externalFunctions.streamWrite, listOf(arrayValue, byteSize, arraySizeAsLong, handle))

		constructor.buildReturn()
	}

	private fun flush(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor) //TODO error handling
		val thisObject = context.getThisParameter(constructor)

		val handleProperty = context.standardLibrary.nativeOutputStream.getNativeValueProperty(constructor, thisObject)
		val handle = constructor.buildLoad(constructor.pointerType, handleProperty, "handle")

		constructor.buildFunctionCall(context.externalFunctions.streamFlush, listOf(handle))

		constructor.buildReturn()
	}
}
