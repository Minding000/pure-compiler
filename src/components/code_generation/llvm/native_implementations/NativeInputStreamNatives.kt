package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.NativeRegistry

class NativeInputStreamNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("NativeInputStream.readByte(): Byte", ::readByte)
		registry.registerNativeImplementation("NativeInputStream.readBytes(Int): ByteArray", ::readBytes)
	}

	private fun readByte(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor) //TODO error handling
		val thisObject = context.getThisParameter(constructor)

		val runtimeClass = context.standardLibrary.nativeInputStream
		val handleProperty =
			constructor.buildGetPropertyPointer(runtimeClass.struct, thisObject, runtimeClass.valuePropertyIndex, "handleProperty")
		val handle = constructor.buildLoad(constructor.pointerType, handleProperty, "handle")
		val byteAsInteger = constructor.buildFunctionCall(context.externalFunctions.streamReadByte, listOf(handle), "byteAsInteger")

		val endOfFileIndicator = constructor.buildInt32(-1)
		val hasFailed = constructor.buildSignedIntegerEqualTo(byteAsInteger, endOfFileIndicator, "hasFailed")
		val okBlock = constructor.createBlock(llvmFunctionValue, "ok")
		val errorBlock = constructor.createBlock(llvmFunctionValue, "error")
		constructor.buildJump(hasFailed, errorBlock, okBlock)
		constructor.select(okBlock)
		val byte = constructor.buildCastFromIntegerToByte(byteAsInteger, "byte")
		constructor.buildReturn(byte)
		constructor.select(errorBlock)
		val errorCode = constructor.buildFunctionCall(context.externalFunctions.streamError, listOf(handle), "errorCode")
		context.panic(constructor, "Failed to read byte: %i", errorCode)
		constructor.markAsUnreachable()
	}

	private fun readBytes(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor) //TODO error handling
		val thisObject = context.getThisParameter(constructor)
		val amount = constructor.getLastParameter()

		val runtimeClass = context.standardLibrary.nativeInputStream
		val handleProperty =
			constructor.buildGetPropertyPointer(runtimeClass.struct, thisObject, runtimeClass.valuePropertyIndex, "handleProperty")
		val handle = constructor.buildLoad(constructor.pointerType, handleProperty, "handle")

		val byteArrayRuntimeClass = context.standardLibrary.byteArray
		val byteArray = constructor.buildHeapAllocation(byteArrayRuntimeClass.struct, "byteArrayObject")
		val arrayClassDefinitionProperty = constructor.buildGetPropertyPointer(byteArrayRuntimeClass.struct, byteArray,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "arrayClassDefinitionProperty")
		constructor.buildStore(byteArrayRuntimeClass.classDefinition, arrayClassDefinitionProperty)
		val desiredNumberOfBytes = constructor.buildCastFromIntegerToLong(amount, "desiredNumberOfBytes")

		val arrayValueProperty =
			constructor.buildGetPropertyPointer(byteArrayRuntimeClass.struct, byteArray, byteArrayRuntimeClass.valuePropertyIndex,
			"arrayValueProperty")
		val buffer = constructor.buildHeapArrayAllocation(constructor.byteType, amount, "byteArray")
		constructor.buildStore(buffer, arrayValueProperty)

		val byteSize = constructor.buildInt64(1)
		val actualNumberOfBytesAsLong =
			constructor.buildFunctionCall(context.externalFunctions.streamRead, listOf(buffer, byteSize, desiredNumberOfBytes, handle),
				"actualNumberOfBytesAsLong")

		val actualNumberOfBytes = constructor.buildCastFromIntegerToLong(actualNumberOfBytesAsLong, "actualNumberOfBytes")

		val arraySizeProperty = context.resolveMember(constructor, byteArray, "size")
		constructor.buildStore(actualNumberOfBytes, arraySizeProperty)
		constructor.buildReturn(byteArray)
	}
}
