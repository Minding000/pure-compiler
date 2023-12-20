package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.ValueConverter
import components.semantic_model.context.Context

object IntNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
		context.registerNativeImplementation("Int++", ::increment)
		context.registerNativeImplementation("Int--", ::decrement)
		context.registerNativeImplementation("Int-: Self", ::negative)
		context.registerNativeImplementation("Int + Self: Self", ::plus)
		context.registerNativeImplementation("Int - Self: Self", ::minus)
		context.registerNativeImplementation("Int * Self: Self", ::times)
		context.registerNativeImplementation("Int / Self: Self", ::dividedBy)
		context.registerNativeImplementation("Int += Self", ::add)
		context.registerNativeImplementation("Int -= Self", ::subtract)
		context.registerNativeImplementation("Int *= Self", ::multiply)
		context.registerNativeImplementation("Int /= Self", ::divide)
		context.registerNativeImplementation("Int < Self: Bool", ::lessThan)
		context.registerNativeImplementation("Int > Self: Bool", ::greaterThan)
		context.registerNativeImplementation("Int <= Self: Bool", ::lessThanOrEqualTo)
		context.registerNativeImplementation("Int >= Self: Bool", ::greaterThanOrEqualTo)
	}

	private fun increment(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisInt = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisInt,
			context.integerValueIndex, "thisValueProperty")
		val thisPrimitiveInt = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveInt")
		val result = constructor.buildIntegerAddition(thisPrimitiveInt, constructor.buildInt32(1), "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun decrement(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisInt = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisInt,
			context.integerValueIndex, "thisValueProperty")
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
		val thisInt = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisInt,
			context.integerValueIndex, "thisValueProperty")
		val thisPrimitiveInt = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveInt")
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerAddition(thisPrimitiveInt, parameterPrimitiveInt, "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun subtract(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisInt = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisInt,
			context.integerValueIndex, "thisValueProperty")
		val thisPrimitiveInt = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveInt")
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerSubtraction(thisPrimitiveInt, parameterPrimitiveInt, "subtractionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun multiply(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisInt = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisInt,
			context.integerValueIndex, "thisValueProperty")
		val thisPrimitiveInt = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveInt")
		val parameterPrimitiveInt = ValueConverter.unwrapInteger(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerMultiplication(thisPrimitiveInt, parameterPrimitiveInt, "multiplicationResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun divide(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisInt = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisInt,
			context.integerValueIndex, "thisValueProperty")
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
}
