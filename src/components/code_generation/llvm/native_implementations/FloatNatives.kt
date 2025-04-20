package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import errors.internal.CompilerError

class FloatNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativePrimitiveInitializer("Float(Byte): Self", ::fromByte)
		registry.registerNativePrimitiveInitializer("Float(Int): Self", ::fromInt)
		registry.registerNativePrimitiveInitializer("Float(Float): Self", ::fromFloat)
		registry.registerNativePrimitiveInitializer("Float(String): Self", ::fromString)
		registry.registerNativeImplementation("Float-: Self", ::negative)
		registry.registerNativeImplementation("Float + Self: Self", ::plus)
		registry.registerNativeImplementation("Float - Self: Self", ::minus)
		registry.registerNativeImplementation("Float * Self: Self", ::times)
		registry.registerNativeImplementation("Float / Self: Self", ::dividedBy)
		registry.registerNativeImplementation("Float += Self", ::add)
		registry.registerNativeImplementation("Float -= Self", ::subtract)
		registry.registerNativeImplementation("Float *= Self", ::multiply)
		registry.registerNativeImplementation("Float /= Self", ::divide)
		registry.registerNativeImplementation("Float < Self: Bool", ::lessThan)
		registry.registerNativeImplementation("Float > Self: Bool", ::greaterThan)
		registry.registerNativeImplementation("Float <= Self: Bool", ::lessThanOrEqualTo)
		registry.registerNativeImplementation("Float >= Self: Bool", ::greaterThanOrEqualTo)
		registry.registerNativeImplementation("Float == Float: Bool", ::equalTo)
		registry.registerNativeImplementation("Float != Float: Bool", ::notEqualTo)
	}

	private fun fromByte(constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Float(Byte): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return constructor.buildCastFromSignedIntegerToFloat(firstParameter, name)
	}

	private fun fromInt(constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Float(Int): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return constructor.buildCastFromSignedIntegerToFloat(firstParameter, name)
	}

	private fun fromFloat(constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Float(Float): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return firstParameter
	}

	private fun fromString(constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Float(String): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")

		val bytesProperty = context.resolveMember(constructor, firstParameter, "bytes")
		val byteArray = constructor.buildLoad(constructor.pointerType, bytesProperty, "byteArray")

		val arrayValueProperty = context.standardLibrary.byteArray.getNativeValueProperty(constructor, byteArray)
		val buffer = constructor.buildLoad(constructor.pointerType, arrayValueProperty, "buffer")

		val double = constructor.buildFunctionCall(context.externalFunctions.parseDouble, listOf(buffer, constructor.nullPointer), "double")
		return constructor.buildCastFromDoubleToFloat(double, "float")
	}

	private fun negative(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val result = constructor.buildFloatNegation(thisPrimitiveFloat, "negationResult")
		constructor.buildReturn(ValueConverter.wrapFloat(context, constructor, result))
	}

	private fun plus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatAddition(thisPrimitiveFloat, parameterPrimitiveFloat, "additionResult")
		constructor.buildReturn(ValueConverter.wrapFloat(context, constructor, result))
	}

	private fun minus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatSubtraction(thisPrimitiveFloat, parameterPrimitiveFloat, "subtractionResult")
		constructor.buildReturn(ValueConverter.wrapFloat(context, constructor, result))
	}

	private fun times(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatMultiplication(thisPrimitiveFloat, parameterPrimitiveFloat, "multiplicationResult")
		constructor.buildReturn(ValueConverter.wrapFloat(context, constructor, result))
	}

	private fun dividedBy(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatDivision(thisPrimitiveFloat, parameterPrimitiveFloat, "divisionResult")
		constructor.buildReturn(ValueConverter.wrapFloat(context, constructor, result))
	}

	private fun add(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.float.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatAddition(thisPrimitiveFloat, parameterPrimitiveFloat, "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun subtract(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.float.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatSubtraction(thisPrimitiveFloat, parameterPrimitiveFloat, "subtractionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun multiply(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.float.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatMultiplication(thisPrimitiveFloat, parameterPrimitiveFloat, "multiplicationResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun divide(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.float.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatDivision(thisPrimitiveFloat, parameterPrimitiveFloat, "divisionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun lessThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatLessThan(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatGreaterThan(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun lessThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatLessThanOrEqualTo(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatGreaterThanOrEqualTo(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun equalTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildFloatEqualTo(thisPrimitiveFloat, parameterPrimitiveFloat, "equalToResult")
		constructor.buildReturn(result)
	}

	private fun notEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildFloatNotEqualTo(thisPrimitiveFloat, parameterPrimitiveFloat, "notEqualToResult")
		constructor.buildReturn(result)
	}
}
