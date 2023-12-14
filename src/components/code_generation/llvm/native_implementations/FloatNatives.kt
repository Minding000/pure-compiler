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
		val thisPropertyPointer = constructor.buildGetPropertyPointer(
			context.floatTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.floatValueIndex, "_propertyPointer")
		return constructor.buildLoad(constructor.floatType, thisPropertyPointer, "_primitiveValue")
	}

	private fun wrap(constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val newFloatAddress = constructor.buildHeapAllocation(context.floatTypeDeclaration?.llvmType, "newFloatAddress")
		val floatClassDefinitionPointer = constructor.buildGetPropertyPointer(
			context.floatTypeDeclaration?.llvmType,
			newFloatAddress, Context.CLASS_DEFINITION_PROPERTY_INDEX, "floatClassDefinitionPointer")
		val floatClassDefinitionAddress = context.floatTypeDeclaration?.llvmClassDefinitionAddress
			?: throw CompilerError("Missing float type declaration.")
		constructor.buildStore(floatClassDefinitionAddress, floatClassDefinitionPointer)
		val valuePointer = constructor.buildGetPropertyPointer(
			context.floatTypeDeclaration?.llvmType, newFloatAddress,
			context.floatValueIndex, "valuePointer")
		constructor.buildStore(primitiveLlvmValue, valuePointer)
		return newFloatAddress
	}

	private fun negative(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val result = constructor.buildFloatNegation(thisPrimitiveValue, "_negationResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun plus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatAddition(thisPrimitiveValue, parameterPrimitiveValue, "_additionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun minus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatSubtraction(thisPrimitiveValue, parameterPrimitiveValue, "_subtractionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun times(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatMultiplication(thisPrimitiveValue, parameterPrimitiveValue, "_multiplicationResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun dividedBy(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatDivision(thisPrimitiveValue, parameterPrimitiveValue, "_divisionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun add(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(
			context.floatTypeDeclaration?.llvmType, thisObjectPointer,
			context.floatValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.floatType, thisPropertyPointer, "_thisPrimitiveValue")
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatAddition(thisPrimitiveValue, parameterPrimitiveValue, "_additionResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}

	private fun subtract(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(
			context.floatTypeDeclaration?.llvmType, thisObjectPointer,
			context.floatValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.floatType, thisPropertyPointer, "_thisPrimitiveValue")
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatSubtraction(thisPrimitiveValue, parameterPrimitiveValue, "_subtractionResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}

	private fun multiply(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(
			context.floatTypeDeclaration?.llvmType, thisObjectPointer,
			context.floatValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.floatType, thisPropertyPointer, "_thisPrimitiveValue")
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatMultiplication(thisPrimitiveValue, parameterPrimitiveValue, "_multiplicationResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}

	private fun divide(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(
			context.floatTypeDeclaration?.llvmType, thisObjectPointer,
			context.floatValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.floatType, thisPropertyPointer, "_thisPrimitiveValue")
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatDivision(thisPrimitiveValue, parameterPrimitiveValue, "_divisionResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}

	private fun lessThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatLessThan(thisPrimitiveValue, parameterPrimitiveValue, "_comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatGreaterThan(thisPrimitiveValue, parameterPrimitiveValue, "_comparisonResult")
		constructor.buildReturn(result)
	}

	private fun lessThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatLessThanOrEqualTo(thisPrimitiveValue, parameterPrimitiveValue, "_comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatGreaterThanOrEqualTo(thisPrimitiveValue, parameterPrimitiveValue,
			"_comparisonResult")
		constructor.buildReturn(result)
	}
}
