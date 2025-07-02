package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.general.SemanticModel

class StringNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("String(Float)", ::fromFloat)
	}

	private fun fromFloat(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val thisParameter = context.getThisParameter(constructor, llvmFunctionValue)
		val float = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val double = constructor.buildCastFromFloatToDouble(float, "_double")
		val format = constructor.buildGlobalAsciiCharArray("floatToStringFormat", "%.9g")
		val sizeWithoutTermination = constructor.buildFunctionCall(context.externalFunctions.printSize,
			listOf(constructor.nullPointer, constructor.buildInt64(0), format, double), "sizeWithoutTermination")
		val size = constructor.buildIntegerAddition(sizeWithoutTermination, constructor.buildInt32(1), "size")

		val byteArrayRuntimeClass = context.standardLibrary.byteArray
		val byteArray = constructor.buildHeapAllocation(byteArrayRuntimeClass.struct, "_byteArray")
		byteArrayRuntimeClass.setClassDefinition(constructor, byteArray)
		val arraySizeProperty = context.resolveMember(constructor, byteArray, "size")
		constructor.buildStore(sizeWithoutTermination, arraySizeProperty)

		val arrayValueProperty = byteArrayRuntimeClass.getNativeValueProperty(constructor, byteArray)
		val buffer = constructor.buildHeapArrayAllocation(constructor.byteType, size, "characters")
		constructor.buildFunctionCall(context.externalFunctions.printToBuffer, listOf(buffer, format, double))
		constructor.buildStore(buffer, arrayValueProperty)

		val thisBytesProperty = context.resolveMember(constructor, thisParameter, "bytes")
		constructor.buildStore(byteArray, thisBytesProperty)
	}
}
