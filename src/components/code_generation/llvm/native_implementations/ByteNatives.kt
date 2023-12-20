package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import errors.internal.CompilerError

object ByteNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
		context.registerNativeImplementation("Byte++", ::increment)
		context.registerNativeImplementation("Byte--", ::decrement)
		context.registerNativeImplementation("Byte-: Self", ::negative)
		context.registerNativeImplementation("Byte + Self: Self", ::plus)
		context.registerNativeImplementation("Byte - Self: Self", ::minus)
		context.registerNativeImplementation("Byte * Self: Self", ::times)
		context.registerNativeImplementation("Byte / Self: Self", ::dividedBy)
		context.registerNativeImplementation("Byte += Self", ::add)
		context.registerNativeImplementation("Byte -= Self", ::subtract)
		context.registerNativeImplementation("Byte *= Self", ::multiply)
		context.registerNativeImplementation("Byte /= Self", ::divide)
		context.registerNativeImplementation("Byte < Self: Bool", ::lessThan)
		context.registerNativeImplementation("Byte > Self: Bool", ::greaterThan)
		context.registerNativeImplementation("Byte <= Self: Bool", ::lessThanOrEqualTo)
		context.registerNativeImplementation("Byte >= Self: Bool", ::greaterThanOrEqualTo)
	}

	private fun unwrap(constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val valueProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.byteValueIndex, "_valueProperty")
		return constructor.buildLoad(constructor.byteType, valueProperty, "_value")
	}

	private fun wrap(constructor: LlvmConstructor, primitiveByteValue: LlvmValue): LlvmValue {
		val byte = constructor.buildHeapAllocation(context.byteTypeDeclaration?.llvmType, "_byte")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, byte,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "_classDefinitionProperty")
		val classDefinition = context.byteTypeDeclaration?.llvmClassDefinition
			?: throw CompilerError("Missing byte type declaration.")
		constructor.buildStore(classDefinition, classDefinitionProperty)
		val valueProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, byte, context.byteValueIndex,
			"_valueProperty")
		constructor.buildStore(primitiveByteValue, valueProperty)
		return byte
	}

	private fun increment(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisByte = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, thisByte, context.byteValueIndex,
			"thisValueProperty")
		val thisPrimitiveByte = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveByte")
		val result = constructor.buildIntegerAddition(thisPrimitiveByte, constructor.buildInt32(1), "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun decrement(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisByte = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, thisByte, context.byteValueIndex,
			"thisValueProperty")
		val thisPrimitiveByte = constructor.buildLoad(constructor.i32Type, thisValueProperty, "thisPrimitiveByte")
		val result = constructor.buildIntegerSubtraction(thisPrimitiveByte, constructor.buildInt32(1), "subtractionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun negative(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = unwrap(constructor, context.getThisParameter(constructor))
		val result = constructor.buildIntegerNegation(thisPrimitiveByte, "negationResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun plus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerAddition(thisPrimitiveByte, parameterPrimitiveByte, "additionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun minus(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerSubtraction(thisPrimitiveByte, parameterPrimitiveByte, "subtractionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun times(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerMultiplication(thisPrimitiveByte, parameterPrimitiveByte, "multiplicationResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun dividedBy(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerDivision(thisPrimitiveByte, parameterPrimitiveByte, "divisionResult")
		constructor.buildReturn(wrap(constructor, result))
	}

	private fun add(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisByte = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, thisByte, context.byteValueIndex,
			"thisValueProperty")
		val thisPrimitiveByte = constructor.buildLoad(constructor.byteType, thisValueProperty, "thisPrimitiveByte")
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerAddition(thisPrimitiveByte, parameterPrimitiveByte, "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun subtract(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisByte = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, thisByte, context.byteValueIndex,
			"thisValueProperty")
		val thisPrimitiveByte = constructor.buildLoad(constructor.byteType, thisValueProperty, "thisPrimitiveByte")
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerSubtraction(thisPrimitiveByte, parameterPrimitiveByte, "subtractionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun multiply(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisByte = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, thisByte, context.byteValueIndex,
			"thisValueProperty")
		val thisPrimitiveByte = constructor.buildLoad(constructor.byteType, thisValueProperty, "thisPrimitiveByte")
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildIntegerMultiplication(thisPrimitiveByte, parameterPrimitiveByte, "multiplicationResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun divide(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisByte = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, thisByte, context.byteValueIndex,
			"thisValueProperty")
		val thisPrimitiveByte = constructor.buildLoad(constructor.byteType, thisValueProperty, "thisPrimitiveByte")
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerDivision(thisPrimitiveByte, parameterPrimitiveByte, "divisionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun lessThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerLessThan(thisPrimitiveByte, parameterPrimitiveByte, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThan(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerGreaterThan(thisPrimitiveByte, parameterPrimitiveByte, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun lessThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerLessThanOrEqualTo(thisPrimitiveByte, parameterPrimitiveByte, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThanOrEqualTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveByte = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveByte = unwrap(constructor, constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildSignedIntegerGreaterThanOrEqualTo(thisPrimitiveByte, parameterPrimitiveByte, "comparisonResult")
		constructor.buildReturn(result)
	}
}
