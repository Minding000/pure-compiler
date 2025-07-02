package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.general.SemanticModel
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

	private fun fromByte(model: SemanticModel, constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Float(Byte): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return constructor.buildCastFromSignedIntegerToFloat(firstParameter, name)
	}

	private fun fromInt(model: SemanticModel, constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Float(Int): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return constructor.buildCastFromSignedIntegerToFloat(firstParameter, name)
	}

	private fun fromFloat(model: SemanticModel, constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Float(Float): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")
		return firstParameter
	}

	private fun fromString(model: SemanticModel, constructor: LlvmConstructor, parameters: List<LlvmValue?>): LlvmValue {
		val name = "Float(String): Self"
		if(parameters.size != 1)
			throw CompilerError("'$name' declares ${parameters.size} parameters, but 1 is expected")
		val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '$name' is null.")

		val bytesProperty = context.resolveMember(constructor, firstParameter, "bytes")
		val byteArray = constructor.buildLoad(constructor.pointerType, bytesProperty, "byteArray")

		val sizeProperty = context.resolveMember(constructor, byteArray, "size")
		val size = constructor.buildLoad(constructor.i32Type, sizeProperty, "size")
		val isStringEmpty = constructor.buildSignedIntegerEqualTo(size, constructor.buildInt32(0), "isStringEmpty")
		val emptyBlock = constructor.createBlock("empty")
		val notEmptyBlock = constructor.createBlock("notEmpty")
		constructor.buildJump(isStringEmpty, emptyBlock, notEmptyBlock)
		constructor.select(emptyBlock)
		val emptyStringMessage = "Failed to parse float: Empty string"
		if(context.nativeRegistry.has(SpecialType.EXCEPTION)) {
			context.raiseException(constructor, model, emptyStringMessage, true)
		} else {
			context.panic(constructor, emptyStringMessage)
			constructor.markAsUnreachable()
		}
		constructor.select(notEmptyBlock)
		val arrayValueProperty = context.standardLibrary.byteArray.getNativeValueProperty(constructor, byteArray)
		val buffer = constructor.buildLoad(constructor.pointerType, arrayValueProperty, "buffer")

		val firstInvalidCharacterStringAddress =
			constructor.buildStackAllocation(constructor.pointerType, "firstInvalidCharacterStringAddress")
		val double =
			constructor.buildFunctionCall(context.externalFunctions.parseDouble, listOf(buffer, firstInvalidCharacterStringAddress),
				"double")

		val firstInvalidCharacterAddress =
			constructor.buildLoad(constructor.pointerType, firstInvalidCharacterStringAddress, "firstInvalidCharacterAddress")
		val firstInvalidCharacter = constructor.buildLoad(constructor.byteType, firstInvalidCharacterAddress, "firstInvalidCharacter")
		val isNullTerminator = constructor.buildSignedIntegerEqualTo(firstInvalidCharacter, constructor.buildByte(0), "isNullTerminator")
		val successBlock = constructor.createBlock("success")
		val errorBlock = constructor.createBlock("error")
		constructor.buildJump(isNullTerminator, successBlock, errorBlock)
		constructor.select(errorBlock)
		val invalidCharacterMessageTemplate = "Failed to parse float: Invalid character '%.1s'"
		if(context.nativeRegistry.has(SpecialType.EXCEPTION)) {
			//TODO multibyte support
			val templateLengthExpansion = -3
			val exceptionParameter = context.getExceptionParameter(constructor)
			val templateCharArray = constructor.buildGlobalAsciiCharArray("floatInvalidCharacterMessage", invalidCharacterMessageTemplate)
			val messageLengthWithoutTermination = constructor.buildInt32(invalidCharacterMessageTemplate.length + templateLengthExpansion)
			val messageLength =
				constructor.buildIntegerAddition(messageLengthWithoutTermination, constructor.buildInt32(1), "messageLength")
			val messageCharArray = constructor.buildHeapArrayAllocation(constructor.byteType, messageLength, "message")
			constructor.buildFunctionCall(context.externalFunctions.printToBuffer,
				listOf(messageCharArray, templateCharArray, firstInvalidCharacterAddress))
			val stringObject = constructor.buildFunctionCall(context.runtimeFunctions.createString,
				listOf(exceptionParameter, messageCharArray, messageLengthWithoutTermination), "messageString")
			context.raiseException(constructor, model, stringObject, true)
		} else {
			context.panic(constructor, invalidCharacterMessageTemplate, firstInvalidCharacterAddress)
			constructor.markAsUnreachable()
		}
		constructor.select(successBlock)
		return constructor.buildCastFromDoubleToFloat(double, "float")
	}

	private fun negative(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val result = constructor.buildFloatNegation(thisPrimitiveFloat, "negationResult")
		constructor.buildReturn(ValueConverter.wrapFloat(context, constructor, result))
	}

	private fun plus(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatAddition(thisPrimitiveFloat, parameterPrimitiveFloat, "additionResult")
		constructor.buildReturn(ValueConverter.wrapFloat(context, constructor, result))
	}

	private fun minus(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatSubtraction(thisPrimitiveFloat, parameterPrimitiveFloat, "subtractionResult")
		constructor.buildReturn(ValueConverter.wrapFloat(context, constructor, result))
	}

	private fun times(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatMultiplication(thisPrimitiveFloat, parameterPrimitiveFloat, "multiplicationResult")
		constructor.buildReturn(ValueConverter.wrapFloat(context, constructor, result))
	}

	//TODO add division by zero
	private fun dividedBy(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatDivision(thisPrimitiveFloat, parameterPrimitiveFloat, "divisionResult")
		constructor.buildReturn(ValueConverter.wrapFloat(context, constructor, result))
	}

	private fun add(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.float.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatAddition(thisPrimitiveFloat, parameterPrimitiveFloat, "additionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun subtract(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.float.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatSubtraction(thisPrimitiveFloat, parameterPrimitiveFloat, "subtractionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun multiply(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.float.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatMultiplication(thisPrimitiveFloat, parameterPrimitiveFloat, "multiplicationResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	//TODO add division by zero
	private fun divide(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.float.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisPrimitiveFloat = constructor.buildLoad(constructor.floatType, thisValueProperty, "thisPrimitiveFloat")
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatDivision(thisPrimitiveFloat, parameterPrimitiveFloat, "divisionResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}

	private fun lessThan(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatLessThan(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThan(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatGreaterThan(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun lessThanOrEqualTo(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatLessThanOrEqualTo(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun greaterThanOrEqualTo(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, constructor.getParameter(llvmFunctionValue,
			Context.VALUE_PARAMETER_OFFSET))
		val result = constructor.buildFloatGreaterThanOrEqualTo(thisPrimitiveFloat, parameterPrimitiveFloat, "comparisonResult")
		constructor.buildReturn(result)
	}

	private fun equalTo(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildFloatEqualTo(thisPrimitiveFloat, parameterPrimitiveFloat, "equalToResult")
		constructor.buildReturn(result)
	}

	private fun notEqualTo(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveFloat = ValueConverter.unwrapFloat(context, constructor, context.getThisParameter(constructor))
		val parameterPrimitiveFloat = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildFloatNotEqualTo(thisPrimitiveFloat, parameterPrimitiveFloat, "notEqualToResult")
		constructor.buildReturn(result)
	}
}
