package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import errors.internal.CompilerError

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

	private fun unwrap(constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val thisPropertyPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.integerValueIndex, "_propertyPointer")
		return constructor.buildLoad(constructor.i32Type, thisPropertyPointer, "_primitiveValue")
	}

	private fun wrap(constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val newIntegerAddress = constructor.buildHeapAllocation(context.integerTypeDeclaration?.llvmType, "newIntegerAddress")
		val integerClassDefinitionPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType,
			newIntegerAddress, Context.CLASS_DEFINITION_PROPERTY_INDEX, "integerClassDefinitionPointer")
		val integerClassDefinitionAddress = context.integerTypeDeclaration?.llvmClassDefinitionAddress
			?: throw CompilerError("Missing integer type declaration.")
		constructor.buildStore(integerClassDefinitionAddress, integerClassDefinitionPointer)
		val valuePointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, newIntegerAddress,
			context.integerValueIndex, "valuePointer")
		constructor.buildStore(primitiveLlvmValue, valuePointer)
		return newIntegerAddress
	}

	private fun increment(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisObjectPointer,
			context.integerValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.i32Type, thisPropertyPointer, "_thisPrimitiveValue")
		val result = constructor.buildIntegerAddition(thisPrimitiveValue, constructor.buildInt32(1), "_additionResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}

	private fun decrement(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisObjectPointer,
			context.integerValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.i32Type, thisPropertyPointer, "_thisPrimitiveValue")
		val result = constructor.buildIntegerSubtraction(thisPrimitiveValue, constructor.buildInt32(1), "_subtractionResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}

	private fun negative(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val result = constructor.buildIntegerNegation(thisPrimitiveValue, "_negationResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun plus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerAddition(thisPrimitiveValue, parameterPrimitiveValue, "_additionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun minus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerSubtraction(thisPrimitiveValue, parameterPrimitiveValue, "_subtractionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun times(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerMultiplication(thisPrimitiveValue, parameterPrimitiveValue, "_multiplicationResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun dividedBy(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerDivision(thisPrimitiveValue, parameterPrimitiveValue, "_divisionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun add(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisObjectPointer,
			context.integerValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.i32Type, thisPropertyPointer, "_thisPrimitiveValue")
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerAddition(thisPrimitiveValue, parameterPrimitiveValue, "_additionResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}

	private fun subtract(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisObjectPointer,
			context.integerValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.i32Type, thisPropertyPointer, "_thisPrimitiveValue")
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerSubtraction(thisPrimitiveValue, parameterPrimitiveValue, "_subtractionResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}

	private fun multiply(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisObjectPointer,
			context.integerValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.i32Type, thisPropertyPointer, "_thisPrimitiveValue")
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerMultiplication(thisPrimitiveValue, parameterPrimitiveValue, "_multiplicationResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}

	private fun divide(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisObjectPointer,
			context.integerValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.i32Type, thisPropertyPointer, "_thisPrimitiveValue")
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerDivision(thisPrimitiveValue, parameterPrimitiveValue, "_divisionResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}

	private fun lessThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerLessThan(thisPrimitiveValue, parameterPrimitiveValue, "_comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerGreaterThan(thisPrimitiveValue, parameterPrimitiveValue, "_comparisonResult")
		constructor.buildReturn(result)
	}

	private fun lessThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerLessThanOrEqualTo(thisPrimitiveValue, parameterPrimitiveValue, "_comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveValue = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerGreaterThanOrEqualTo(thisPrimitiveValue, parameterPrimitiveValue,
			"_comparisonResult")
		constructor.buildReturn(result)
	}
}
