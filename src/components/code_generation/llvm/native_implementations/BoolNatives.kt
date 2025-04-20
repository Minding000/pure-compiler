package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import errors.internal.CompilerError

class BoolNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativePrimitiveInitializer("Bool(Bool): Self", ::fromBool)
		registry.registerNativeImplementation("Bool!: Bool", ::negate)
		registry.registerNativeImplementation("Bool and Bool: Bool", ::and)
		registry.registerNativeImplementation("Bool or Bool: Bool", ::or)
		registry.registerNativeImplementation("Bool.toggle()", ::toggle)
		registry.registerNativeImplementation("Bool == Bool: Bool", ::equalTo)
		registry.registerNativeImplementation("Bool != Bool: Bool", ::notEqualTo)
	}

	private fun fromBool(constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Bool(Bool): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return firstParameter
	}

	private fun negate(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveBool = ValueConverter.unwrapBool(context, constructor, context.getThisParameter(constructor))
		val result = constructor.buildBooleanNegation(thisPrimitiveBool, "negationResult")
		constructor.buildReturn(result)
	}

	private fun and(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveBool = ValueConverter.unwrapBool(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveBool = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildAnd(thisPrimitiveBool, parameterPrimitiveBool, "andResult")
		constructor.buildReturn(result)
	}

	private fun or(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveBool = ValueConverter.unwrapBool(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveBool = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildOr(thisPrimitiveBool, parameterPrimitiveBool, "orResult")
		constructor.buildReturn(result)
	}

	private fun toggle(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.boolean.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveBool = constructor.buildLoad(constructor.booleanType, thisValueProperty, "thisPrimitiveBool")
		val result = constructor.buildBooleanNegation(thisPrimitiveBool, "negationResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun equalTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveBool = ValueConverter.unwrapBool(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveBool = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildBooleanEqualTo(thisPrimitiveBool, parameterPrimitiveBool, "equalToResult")
		constructor.buildReturn(result)
	}

	private fun notEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveBool = ValueConverter.unwrapBool(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveBool = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildBooleanNotEqualTo(thisPrimitiveBool, parameterPrimitiveBool, "notEqualToResult")
		constructor.buildReturn(result)
	}
}
