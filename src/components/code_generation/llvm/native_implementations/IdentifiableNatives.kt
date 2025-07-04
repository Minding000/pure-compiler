package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.general.SemanticModel

class IdentifiableNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("get stringRepresentation: String", ::stringRepresentation)
		registry.registerNativeImplementation("Identifiable === Any?: Bool", ::identicalTo)
	}

	private fun stringRepresentation(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor, llvmFunctionValue)
		val thisIdentifiable = context.getThisParameter(constructor, llvmFunctionValue)

		val format = constructor.buildGlobalAsciiCharArray("pointerToStringFormat", "%p")
		val sizeWithoutTermination = constructor.buildFunctionCall(context.externalFunctions.printSize,
			listOf(constructor.nullPointer, constructor.buildInt64(0), format, thisIdentifiable), "sizeWithoutTermination")
		val size = constructor.buildIntegerAddition(sizeWithoutTermination, constructor.buildInt32(1), "size")
		context.printDebugLine(constructor, "sizeWithoutTermination: %d", sizeWithoutTermination)
		context.printDebugLine(constructor, "size: %d", size)

		val byteArrayRuntimeClass = context.standardLibrary.byteArray
		val byteArray = constructor.buildHeapAllocation(byteArrayRuntimeClass.struct, "_byteArray")
		byteArrayRuntimeClass.setClassDefinition(constructor, byteArray)
		val arraySizeProperty = context.resolveMember(constructor, byteArray, "size")
		constructor.buildStore(sizeWithoutTermination, arraySizeProperty)

		val arrayValueProperty = byteArrayRuntimeClass.getNativeValueProperty(constructor, byteArray)
		val buffer = constructor.buildHeapArrayAllocation(constructor.byteType, size, "characters")
		constructor.buildFunctionCall(context.externalFunctions.printToBuffer, listOf(buffer, format, thisIdentifiable))
		constructor.buildStore(buffer, arrayValueProperty)

		val stringAddress = constructor.buildHeapAllocation(context.standardLibrary.stringTypeDeclaration.llvmType, "_stringAddress")
		val stringClassDefinitionProperty =
			constructor.buildGetPropertyPointer(context.standardLibrary.stringTypeDeclaration.llvmType, stringAddress,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "_stringClassDefinitionProperty")
		val stringClassDefinition = context.standardLibrary.stringTypeDeclaration.llvmClassDefinition
		constructor.buildStore(stringClassDefinition, stringClassDefinitionProperty)
		val parameters = listOf(exceptionAddress, stringAddress, byteArray)
		constructor.buildFunctionCall(context.standardLibrary.stringByteArrayInitializer, parameters)

		constructor.buildReturn(stringAddress)
	}

	private fun identicalTo(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisIdentifiable = context.getThisParameter(constructor)
		val parameterAny = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildPointerEqualTo(thisIdentifiable, parameterAny, "equalsResult")
		constructor.buildReturn(result)
	}
}
