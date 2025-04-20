package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import errors.internal.CompilerError

class IntNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativePrimitiveInitializer("Int(Byte): Self", ::fromByte)
		registry.registerNativePrimitiveInitializer("Int(Int): Self", ::fromInt)
		registry.registerNativeImplementation("Int.toThePowerOf(Int): Int", ::toThePowerOf)
		registry.registerNativeImplementation("Int++", ::increment)
		registry.registerNativeImplementation("Int--", ::decrement)
		registry.registerNativeImplementation("Int-: Self", ::negative)
		registry.registerNativeImplementation("Int + Self: Self", ::plus)
		registry.registerNativeImplementation("Int - Self: Self", ::minus)
		registry.registerNativeImplementation("Int * Self: Self", ::times)
		registry.registerNativeImplementation("Int / Self: Self", ::dividedBy)
		registry.registerNativeImplementation("Int += Self", ::add)
		registry.registerNativeImplementation("Int -= Self", ::subtract)
		registry.registerNativeImplementation("Int *= Self", ::multiply)
		registry.registerNativeImplementation("Int /= Self", ::divide)
		registry.registerNativeImplementation("Int < Self: Bool", ::lessThan)
		registry.registerNativeImplementation("Int > Self: Bool", ::greaterThan)
		registry.registerNativeImplementation("Int <= Self: Bool", ::lessThanOrEqualTo)
		registry.registerNativeImplementation("Int >= Self: Bool", ::greaterThanOrEqualTo)
		registry.registerNativeImplementation("Int == Int: Bool", ::equalTo)
		registry.registerNativeImplementation("Int != Int: Bool", ::notEqualTo)
	}

	private fun fromByte(constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Int(Byte): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return constructor.buildCastFromByteToInteger(firstParameter, name)
	}

	private fun fromInt(constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Int(Int): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return firstParameter
	}

	private fun toThePowerOf(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionAddress = context.getExceptionParameter(constructor)
		val thisInt = context.getThisParameter(constructor)
		val exponent = constructor.getParameter(Context.VALUE_PARAMETER_OFFSET)
		val parameters = listOf(exceptionAddress, ValueConverter.unwrapInteger(context, constructor, thisInt), exponent)
		val primitiveImplementation = context.nativeRegistry.resolvePrimitiveImplementation("Int.toThePowerOf(Int): Int")
		val result = constructor.buildFunctionCall(primitiveImplementation.llvmType, primitiveImplementation.llvmValue, parameters,
			"result")
		constructor.buildReturn(result)
	}

	private fun increment(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.integer.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveInt = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveInt")
		val result = constructor.buildIntegerAddition(thisPrimitiveInt, constructor.buildInt32(1), "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun decrement(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.integer.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveInt = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveInt")
		val result = constructor.buildIntegerSubtraction(thisPrimitiveInt, constructor.buildInt32(1), "subtractionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun negative(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val result = constructor.buildIntegerNegation(thisPrimitiveInt, "negationResult")
		constructor.buildReturn(ValueConverter.wrapInteger(context, constructor, result))
	}

	private fun plus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerAddition(thisPrimitiveInt, parameterPrimitiveInt, "additionResult")
		constructor.buildReturn(ValueConverter.wrapInteger(context, constructor, result))
	}

	private fun minus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerSubtraction(thisPrimitiveInt, parameterPrimitiveInt, "subtractionResult")
		constructor.buildReturn(ValueConverter.wrapInteger(context, constructor, result))
	}

	private fun times(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerMultiplication(thisPrimitiveInt, parameterPrimitiveInt, "multiplicationResult")
		constructor.buildReturn(ValueConverter.wrapInteger(context, constructor, result))
	}

	private fun dividedBy(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerDivision(thisPrimitiveInt, parameterPrimitiveInt, "divisionResult")
		constructor.buildReturn(ValueConverter.wrapInteger(context, constructor, result))
	}

	private fun add(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.integer.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveInt = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveInt")
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerAddition(thisPrimitiveInt, parameterPrimitiveInt, "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun subtract(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.integer.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveInt = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveInt")
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerSubtraction(thisPrimitiveInt, parameterPrimitiveInt, "subtractionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun multiply(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.integer.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveInt = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveInt")
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerMultiplication(thisPrimitiveInt, parameterPrimitiveInt, "multiplicationResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun divide(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.integer.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveInt = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveInt")
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerDivision(thisPrimitiveInt, parameterPrimitiveInt, "divisionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun lessThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerLessThan(thisPrimitiveInt, parameterPrimitiveInt, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerGreaterThan(thisPrimitiveInt, parameterPrimitiveInt, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun lessThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerLessThanOrEqualTo(thisPrimitiveInt, parameterPrimitiveInt, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerGreaterThanOrEqualTo(thisPrimitiveInt, parameterPrimitiveInt, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun equalTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveInt = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildSignedIntegerEqualTo(thisPrimitiveInt, parameterPrimitiveInt, "equalToResult")
		constructor.buildReturn(result)
	}

	private fun notEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveInt = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildSignedIntegerNotEqualTo(thisPrimitiveInt, parameterPrimitiveInt, "notEqualToResult")
		constructor.buildReturn(result)
	}
}
