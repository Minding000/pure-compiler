package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.NativeRegistry
import errors.internal.CompilerError

class ByteNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativePrimitiveInitializer("Byte(Byte): Self", ::fromByte)
		registry.registerNativePrimitiveInitializer("Byte(Int, Int): Self", ::fromInteger)
		registry.registerNativeImplementation("Byte++", ::increment)
		registry.registerNativeImplementation("Byte--", ::decrement)
		registry.registerNativeImplementation("Byte-: Self", ::negative)
		registry.registerNativeImplementation("Byte + Self: Self", ::plus)
		registry.registerNativeImplementation("Byte - Self: Self", ::minus)
		registry.registerNativeImplementation("Byte * Self: Self", ::times)
		registry.registerNativeImplementation("Byte / Self: Self", ::dividedBy)
		registry.registerNativeImplementation("Byte += Self", ::add)
		registry.registerNativeImplementation("Byte -= Self", ::subtract)
		registry.registerNativeImplementation("Byte *= Self", ::multiply)
		registry.registerNativeImplementation("Byte /= Self", ::divide)
		registry.registerNativeImplementation("Byte < Self: Bool", ::lessThan)
		registry.registerNativeImplementation("Byte > Self: Bool", ::greaterThan)
		registry.registerNativeImplementation("Byte <= Self: Bool", ::lessThanOrEqualTo)
		registry.registerNativeImplementation("Byte >= Self: Bool", ::greaterThanOrEqualTo)
		registry.registerNativeImplementation("Byte == Byte: Bool", ::equalTo)
		registry.registerNativeImplementation("Byte != Byte: Bool", ::notEqualTo)
	}

	private fun fromByte(constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Byte(Byte): Self"
		if(parameters.size != 1)
			throw CompilerError("Invalid number of arguments passed to '$name': ${parameters.size}")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return firstParameter
	}

	private fun fromInteger(constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Byte(Int, Int): Self"
		if(parameters.size != 2)
			throw CompilerError("Invalid number of arguments passed to '$name': ${parameters.size}")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return constructor.buildCastFromIntegerToByte(firstParameter, "byte")
	}

	private fun increment(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.byte.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveByte = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveByte")
		val result = constructor.buildIntegerAddition(thisPrimitiveByte, constructor.buildInt32(1), "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun decrement(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.byte.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveByte = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveByte")
		val result = constructor.buildIntegerSubtraction(thisPrimitiveByte, constructor.buildInt32(1), "subtractionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun negative(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val result = constructor.buildIntegerNegation(thisPrimitiveByte, "negationResult")
		constructor.buildReturn(ValueConverter.wrapByte(context, constructor, result))
	}

	private fun plus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerAddition(thisPrimitiveByte, parameterPrimitiveByte, "additionResult")
		constructor.buildReturn(ValueConverter.wrapByte(context, constructor, result))
	}

	private fun minus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerSubtraction(thisPrimitiveByte, parameterPrimitiveByte, "subtractionResult")
		constructor.buildReturn(ValueConverter.wrapByte(context, constructor, result))
	}

	private fun times(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerMultiplication(thisPrimitiveByte, parameterPrimitiveByte, "multiplicationResult")
		constructor.buildReturn(ValueConverter.wrapByte(context, constructor, result))
	}

	private fun dividedBy(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerDivision(thisPrimitiveByte, parameterPrimitiveByte, "divisionResult")
		constructor.buildReturn(ValueConverter.wrapByte(context, constructor, result))
	}

	private fun add(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.byte.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveByte = constructor.buildLoad(constructor.byteType, thisValueProperty, "thisPrimitiveByte")
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerAddition(thisPrimitiveByte, parameterPrimitiveByte, "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun subtract(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.byte.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveByte = constructor.buildLoad(constructor.byteType, thisValueProperty, "thisPrimitiveByte")
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerSubtraction(thisPrimitiveByte, parameterPrimitiveByte, "subtractionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun multiply(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.byte.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveByte = constructor.buildLoad(constructor.byteType, thisValueProperty, "thisPrimitiveByte")
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerMultiplication(thisPrimitiveByte, parameterPrimitiveByte, "multiplicationResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun divide(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.byte.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveByte = constructor.buildLoad(constructor.byteType, thisValueProperty, "thisPrimitiveByte")
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerDivision(thisPrimitiveByte, parameterPrimitiveByte, "divisionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun lessThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerLessThan(thisPrimitiveByte, parameterPrimitiveByte, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerGreaterThan(thisPrimitiveByte, parameterPrimitiveByte, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun lessThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerLessThanOrEqualTo(thisPrimitiveByte, parameterPrimitiveByte, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = ValueConverter.unwrapByte(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerGreaterThanOrEqualTo(thisPrimitiveByte, parameterPrimitiveByte, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun equalTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildSignedIntegerEqualTo(thisPrimitiveByte, parameterPrimitiveByte, "equalToResult")
		constructor.buildReturn(result)
	}

	private fun notEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = ValueConverter.unwrapByte(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildSignedIntegerNotEqualTo(thisPrimitiveByte, parameterPrimitiveByte, "notEqualToResult")
		constructor.buildReturn(result)
	}
}
