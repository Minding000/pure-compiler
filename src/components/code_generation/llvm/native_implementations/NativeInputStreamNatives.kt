package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.NativeRegistry

class NativeInputStreamNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("NativeInputStream.readByte(): Byte", ::readByte)
		registry.registerNativeImplementation("NativeInputStream.readBytes(Int): <Byte>Array", ::readBytes)
	}

	//TODO implement
	private fun readByte(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor)
		val thisObject = context.getThisParameter(constructor)

		constructor.buildReturn(thisObject)
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

		val arrayType = context.arrayDeclarationType
		val byteArray = constructor.buildHeapAllocation(arrayType, "byteArrayObject")
		val arrayClassDefinitionProperty = constructor.buildGetPropertyPointer(arrayType, byteArray,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "arrayClassDefinitionProperty")
		constructor.buildStore(context.arrayClassDefinition, arrayClassDefinitionProperty)
		val desiredNumberOfBytes = constructor.buildCastFromIntegerToLong(amount, "desiredNumberOfBytes")

		val arrayValueProperty = constructor.buildGetPropertyPointer(arrayType, byteArray, context.arrayValueIndex,
			"arrayValueProperty")
		val buffer = constructor.buildHeapArrayAllocation(constructor.byteType, amount, "byteArray")
		constructor.buildStore(buffer, arrayValueProperty)

		val byteSize = constructor.buildInt64(1)
		val actualNumberOfBytes = constructor.buildFunctionCall(context.llvmReadFunctionType, context.llvmReadFunction,
			listOf(buffer, byteSize, desiredNumberOfBytes, fileDescriptor), "actualNumberOfBytes")

		val arraySizeProperty = context.resolveMember(constructor, byteArray, "size")
		constructor.buildStore(actualNumberOfBytes, arraySizeProperty)
		constructor.buildReturn(byteArray)
	}
}
