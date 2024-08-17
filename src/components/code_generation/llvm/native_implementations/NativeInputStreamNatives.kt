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

		val handleProperty = context.standardLibrary.nativeInputStream.getNativeValueProperty(constructor, thisObject)
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

		val handleProperty = context.standardLibrary.nativeInputStream.getNativeValueProperty(constructor, thisObject)
		val handle = constructor.buildLoad(constructor.pointerType, handleProperty, "handle")

		val byteArrayRuntimeClass = context.standardLibrary.byteArray
		val byteArray = constructor.buildHeapAllocation(byteArrayRuntimeClass.struct, "byteArrayObject")
		byteArrayRuntimeClass.setClassDefinition(constructor, byteArray)
		val desiredNumberOfBytes = constructor.buildCastFromIntegerToLong(amount, "desiredNumberOfBytes")

		val arrayValueProperty = byteArrayRuntimeClass.getNativeValueProperty(constructor, byteArray)
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
