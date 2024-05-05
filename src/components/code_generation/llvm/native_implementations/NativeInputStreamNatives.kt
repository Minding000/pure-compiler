package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
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

		val identifierProperty = context.resolveMember(constructor, thisObject, "identifier")
		val identifier = constructor.buildLoad(constructor.i32Type, identifierProperty, "identifier")
		val mode = constructor.buildGlobalAsciiCharArray("NativeInputStream_readMode", "r")
		val fileDescriptor = constructor.buildFunctionCall(context.llvmOpenFunctionType, context.llvmOpenFunction, listOf(identifier, mode),
			"fileDescriptor")

		val byteAsInteger = constructor.buildFunctionCall(context.llvmReadByteFunctionType, context.llvmReadByteFunction,
			listOf(fileDescriptor), "byteAsInteger")

		val byte = constructor.buildCastFromIntegerToByte(byteAsInteger, "byte")
		constructor.buildReturn(byte)
	}

	private fun readBytes(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor) //TODO error handling
		val thisObject = context.getThisParameter(constructor)
		val amount = constructor.getLastParameter()

		val identifierProperty = context.resolveMember(constructor, thisObject, "identifier")
		val identifier = constructor.buildLoad(constructor.i32Type, identifierProperty, "identifier")
		val mode = constructor.buildGlobalAsciiCharArray("NativeInputStream_readMode", "r")
		val fileDescriptor = constructor.buildFunctionCall(context.llvmOpenFunctionType, context.llvmOpenFunction, listOf(identifier, mode),
			"fileDescriptor")

		val arrayType = context.byteArrayDeclarationType
		val byteArray = constructor.buildHeapAllocation(arrayType, "byteArrayObject")
		val arrayClassDefinitionProperty = constructor.buildGetPropertyPointer(arrayType, byteArray,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "arrayClassDefinitionProperty")
		constructor.buildStore(context.byteArrayClassDefinition, arrayClassDefinitionProperty)
		val desiredNumberOfBytes = constructor.buildCastFromIntegerToLong(amount, "desiredNumberOfBytes")

		val arrayValueProperty = constructor.buildGetPropertyPointer(arrayType, byteArray, context.byteArrayValueIndex,
			"arrayValueProperty")
		val buffer = constructor.buildHeapArrayAllocation(constructor.byteType, amount, "byteArray")
		constructor.buildStore(buffer, arrayValueProperty)

		val byteSize = constructor.buildInt64(1)
		val actualNumberOfBytesAsLong = constructor.buildFunctionCall(context.llvmReadFunctionType, context.llvmReadFunction,
			listOf(buffer, byteSize, desiredNumberOfBytes, fileDescriptor), "actualNumberOfBytesAsLong")

		val actualNumberOfBytes = constructor.buildCastFromIntegerToLong(actualNumberOfBytesAsLong, "actualNumberOfBytes")

		val arraySizeProperty = context.resolveMember(constructor, byteArray, "size")
		constructor.buildStore(actualNumberOfBytes, arraySizeProperty)
		constructor.buildReturn(byteArray)
	}
}
