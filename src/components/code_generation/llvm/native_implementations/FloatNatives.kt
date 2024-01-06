package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.ValueConverter
import components.semantic_model.context.Context

object FloatNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
		context.registerNativeInstance("Float.ZERO: Self", ::zero)
		context.registerNativeInstance("Float.ONE: Self", ::one)
		context.registerNativeImplementation("Float-: Self", ::negative)
		context.registerNativeImplementation("Float + Self: Self", ::plus)
		context.registerNativeImplementation("Float - Self: Self", ::minus)
		context.registerNativeImplementation("Float * Self: Self", ::times)
		context.registerNativeImplementation("Float / Self: Self", ::dividedBy)
		context.registerNativeImplementation("Float += Self", ::add)
		context.registerNativeImplementation("Float -= Self", ::subtract)
		context.registerNativeImplementation("Float *= Self", ::multiply)
		context.registerNativeImplementation("Float /= Self", ::divide)
		context.registerNativeImplementation("Float < Self: Bool", ::lessThan)
		context.registerNativeImplementation("Float > Self: Bool", ::greaterThan)
		context.registerNativeImplementation("Float <= Self: Bool", ::lessThanOrEqualTo)
		context.registerNativeImplementation("Float >= Self: Bool", ::greaterThanOrEqualTo)
	}

	private fun zero(constructor: LlvmConstructor): LlvmValue {
		return constructor.buildFloat(0.0)
	}

	private fun one(constructor: LlvmConstructor): LlvmValue {
		return constructor.buildFloat(1.0)
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
		val thisFloat = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType, thisFloat,
			context.floatValueIndex, "thisValueProperty")
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatAddition(thisPrimitiveFloat, parameterPrimitiveFloat, "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun subtract(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisFloat = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType, thisFloat,
			context.floatValueIndex, "thisValueProperty")
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatSubtraction(thisPrimitiveFloat, parameterPrimitiveFloat, "subtractionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun multiply(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisFloat = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType, thisFloat,
			context.floatValueIndex, "thisValueProperty")
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatMultiplication(thisPrimitiveFloat, parameterPrimitiveFloat, "multiplicationResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun divide(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisFloat = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType, thisFloat,
			context.floatValueIndex, "thisValueProperty")
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
}
