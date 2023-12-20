package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import errors.internal.CompilerError

object FloatNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
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

	private fun unwrap(constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val valueProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.floatValueIndex, "_valueProperty")
		return constructor.buildLoad(constructor.floatType, valueProperty, "_value")
	}

	private fun wrap(constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val float = constructor.buildHeapAllocation(context.floatTypeDeclaration?.llvmType, "_float")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType,
			float, Context.CLASS_DEFINITION_PROPERTY_INDEX, "_classDefinitionProperty")
		val classDefinition = context.floatTypeDeclaration?.llvmClassDefinition
			?: throw CompilerError("Missing float type declaration.")
		constructor.buildStore(classDefinition, classDefinitionProperty)
		val valueProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType, float, context.floatValueIndex,
			"_valueProperty")
		constructor.buildStore(primitiveLlvmValue, valueProperty)
		return float
	}

	private fun negative(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = unwrap(constructor, context.getThisParameter(constructor))
		val result = constructor.buildFloatNegation(thisPrimitiveFloat, "negationResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun plus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatAddition(thisPrimitiveFloat, parameterPrimitiveFloat, "additionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun minus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatSubtraction(thisPrimitiveFloat, parameterPrimitiveFloat, "subtractionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun times(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatMultiplication(thisPrimitiveFloat, parameterPrimitiveFloat, "multiplicationResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun dividedBy(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatDivision(thisPrimitiveFloat, parameterPrimitiveFloat, "divisionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun add(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisFloat = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType, thisFloat,
			context.floatValueIndex, "thisValueProperty")
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
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
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
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
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
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
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatDivision(thisPrimitiveFloat, parameterPrimitiveFloat, "divisionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun lessThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatLessThan(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatGreaterThan(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun lessThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatLessThanOrEqualTo(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatGreaterThanOrEqualTo(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}
}
