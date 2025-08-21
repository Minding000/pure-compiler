package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.Llvm
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.general.SemanticModel

class ArrayNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("<Element>Array(...Element)", ::fromPluralType)
		registry.registerNativeImplementation("<Element>Array(Element, Int)", ::fromValueToBeRepeated)
		registry.registerNativeImplementation("Array + <Element>Array: <Element>Array", ::concatenate)
		registry.registerNativeImplementation("Array[Int]: Element", ::get)
		registry.registerNativeImplementation("Array[Int](Element)", ::set)
	}

	private fun fromPluralType(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.pointerType
		val elementList = constructor.buildStackAllocation(context.runtimeStructs.variadicParameterList, "elementList")
		constructor.buildFunctionCall(context.externalFunctions.variableParameterIterationStart, listOf(elementList))
		val elementCount = constructor.getLastParameter(llvmFunctionValue)
		val arrayRuntimeClass = context.standardLibrary.array
		val thisArray = context.getThisParameter(constructor)
		val thisValueProperty = arrayRuntimeClass.getNativeValueProperty(constructor, thisArray)
		val thisValue = constructor.buildHeapArrayAllocation(elementType, elementCount, "thisValue")
		constructor.buildStore(thisValue, thisValueProperty)
		val thisSizeProperty = context.resolveMember(constructor, thisArray, "size")
		constructor.buildStore(elementCount, thisSizeProperty)
		val indexType = constructor.i32Type
		val indexVariable = constructor.buildStackAllocation(indexType, "indexVariable", constructor.buildInt32(0))
		val entryBlock = constructor.createBlock(llvmFunctionValue, "loop_entry")
		val exitBlock = constructor.createBlock(llvmFunctionValue, "loop_exit")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		val index = constructor.buildLoad(indexType, indexVariable, "index")
		val condition = constructor.buildSignedIntegerLessThan(index, elementCount, "condition")
		val bodyBlock = constructor.createBlock(llvmFunctionValue, "loop_body")
		constructor.buildJump(condition, bodyBlock, exitBlock)
		constructor.select(bodyBlock)
		// Variadic parameters are always 64bits in size (at least on Windows).
		// See: https://discourse.llvm.org/t/va-arg-on-windows-64/40780
		val elementMemory = constructor.getCurrentVariadicElement(elementList, constructor.i64Type, "elementMemory")
		val element = constructor.buildCastFromIntegerToPointer(elementMemory, "element")
		val elementElement = constructor.buildGetArrayElementPointer(elementType, thisValue, index, "elementElement")
		constructor.buildStore(element, elementElement)
		val newIndex = constructor.buildIntegerAddition(index, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariable)
		constructor.buildJump(entryBlock)
		constructor.select(exitBlock)
		constructor.buildFunctionCall(context.externalFunctions.variableParameterIterationEnd, listOf(elementList))
	}

	private fun fromValueToBeRepeated(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.pointerType
		val element = constructor.getParameter(Context.VALUE_PARAMETER_OFFSET)
		val elementCount = constructor.getLastParameter(llvmFunctionValue)
		val arrayRuntimeClass = context.standardLibrary.array
		val thisArray = context.getThisParameter(constructor)
		val thisValueProperty = arrayRuntimeClass.getNativeValueProperty(constructor, thisArray)
		val thisValue = constructor.buildHeapArrayAllocation(elementType, elementCount, "thisValue")
		constructor.buildStore(thisValue, thisValueProperty)
		val indexType = constructor.i32Type
		val indexVariable = constructor.buildStackAllocation(indexType, "indexVariable", constructor.buildInt32(0))
		val entryBlock = constructor.createBlock(llvmFunctionValue, "loop_entry")
		val exitBlock = constructor.createBlock(llvmFunctionValue, "loop_exit")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		val index = constructor.buildLoad(indexType, indexVariable, "index")
		val condition = constructor.buildSignedIntegerLessThan(index, elementCount, "condition")
		val bodyBlock = constructor.createBlock(llvmFunctionValue, "loop_body")
		constructor.buildJump(condition, bodyBlock, exitBlock)
		constructor.select(bodyBlock)
		val elementElement = constructor.buildGetArrayElementPointer(elementType, thisValue, index, "elementElement")
		constructor.buildStore(element, elementElement)
		val newIndex = constructor.buildIntegerAddition(index, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariable)
		constructor.buildJump(entryBlock)
		constructor.select(exitBlock)
	}

	private fun concatenate(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.pointerType
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val pointerSizeInBytes = Llvm.getTypeSizeInBytes(constructor.pointerType)
		val pointerSizeInBytesAsSizeType = constructor.buildCastFromLongToSizeType(pointerSizeInBytes, "pointerSizeInBytesAsSizeType")
		val arrayRuntimeClass = context.standardLibrary.array
		val thisArray = context.getThisParameter(constructor)
		val thisArrayValueProperty = arrayRuntimeClass.getNativeValueProperty(constructor, thisArray)
		val thisArrayValue = constructor.buildLoad(constructor.pointerType, thisArrayValueProperty, "thisArrayValue")
		val thisSizeProperty = context.resolveMember(constructor, thisArray, "size")
		val thisSize = constructor.buildLoad(constructor.i32Type, thisSizeProperty, "thisSize")
		val thisSizeAsSizeType = constructor.buildCastFromIntegerToSizeType(thisSize, "thisSizeAsSizeType")
		val thisSizeInBytes = constructor.buildIntegerMultiplication(thisSizeAsSizeType, pointerSizeInBytesAsSizeType, "thisSizeInBytes")
		val parameterArray = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val parameterValueProperty = arrayRuntimeClass.getNativeValueProperty(constructor, parameterArray)
		val parameterValue = constructor.buildLoad(constructor.pointerType, parameterValueProperty, "parameterValue")
		val parameterSizeProperty = context.resolveMember(constructor, parameterArray, "size")
		val parameterSize = constructor.buildLoad(constructor.i32Type, parameterSizeProperty, "parameterSize")
		val parameterSizeAsSizeType = constructor.buildCastFromIntegerToSizeType(parameterSize, "parameterSizeAsSizeType")
		val parameterSizeInBytes = constructor.buildIntegerMultiplication(parameterSizeAsSizeType, pointerSizeInBytesAsSizeType, "parameterSizeInBytes")
		val combinedArray = constructor.buildHeapAllocation(arrayRuntimeClass.struct, "combinedArray")
		arrayRuntimeClass.setClassDefinition(constructor, combinedArray)
		val combinedArraySizeProperty = context.resolveMember(constructor, combinedArray, "size")
		val combinedSize = constructor.buildIntegerAddition(thisSize, parameterSize, "combinedSize")
		constructor.buildStore(combinedSize, combinedArraySizeProperty)
		val combinedValue = constructor.buildHeapArrayAllocation(elementType, combinedSize, "combinedValue")
		constructor.buildFunctionCall(context.externalFunctions.memoryCopy, listOf(combinedValue, thisArrayValue, thisSizeInBytes))
		val offsetCombinedArrayAddress = constructor.buildGetArrayElementPointer(elementType, combinedValue, thisSize,
			"offsetCombinedArrayAddress")
		constructor.buildFunctionCall(context.externalFunctions.memoryCopy,
			listOf(offsetCombinedArrayAddress, parameterValue, parameterSizeInBytes))
		val combinedArrayValueProperty = arrayRuntimeClass.getNativeValueProperty(constructor, combinedArray)
		constructor.buildStore(combinedValue, combinedArrayValueProperty)
		constructor.buildReturn(combinedArray)
	}

	private fun get(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.pointerType
		val elementElement = compileGetElement(model, constructor, llvmFunctionValue)
		val element = constructor.buildLoad(elementType, elementElement, "value")
		constructor.buildReturn(element)
	}

	private fun set(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementElement = compileGetElement(model, constructor, llvmFunctionValue)
		val element = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET + 1)
		constructor.buildStore(element, elementElement)
		constructor.buildReturn()
	}

	private fun compileGetElement(model: SemanticModel, constructor: LlvmConstructor, llvmFunctionValue: LlvmValue): LlvmValue {
		val elementType = constructor.pointerType
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisArray = context.getThisParameter(constructor)
		val arrayRuntimeClass = context.standardLibrary.array
		val thisArrayValueProperty = arrayRuntimeClass.getNativeValueProperty(constructor, thisArray)
		val thisArrayValue = constructor.buildLoad(constructor.pointerType, thisArrayValueProperty, "thisArrayValue")
		val index = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val thisSizeProperty = context.resolveMember(constructor, thisArray, "size")
		val thisSize = constructor.buildLoad(constructor.i32Type, thisSizeProperty, "thisSize")
		val isIndexNegative = constructor.buildSignedIntegerLessThan(index, constructor.buildInt32(0), "isIndexNegative")
		val isTooLarge = constructor.buildSignedIntegerGreaterThanOrEqualTo(index, thisSize, "isTooLarge")
		val isOutOfBounds = constructor.buildOr(isIndexNegative, isTooLarge, "isOutOfBounds")
		val inBoundsBlock = constructor.createBlock("inBounds")
		val outOfBoundsBlock = constructor.createBlock("outOfBounds")
		constructor.buildJump(isOutOfBounds, outOfBoundsBlock, inBoundsBlock)
		constructor.select(outOfBoundsBlock)
		val indexUpperBound = constructor.buildIntegerSubtraction(thisSize, constructor.buildInt32(1), "indexUpperBound")
		val outOfBoundsMessageTemplate = "The index '%d' is outside the arrays bounds (0-%d)"
		if(context.nativeRegistry.has(SpecialType.EXCEPTION)) {
			val exceptionParameter = context.getExceptionParameter(constructor)
			val templateCharArray = constructor.buildGlobalAsciiCharArray("arrayIndexOutOfBoundsMessage", outOfBoundsMessageTemplate)
			val messageLengthWithoutTermination = constructor.buildFunctionCall(context.externalFunctions.printSize,
				listOf(constructor.nullPointer, constructor.buildSizeInt(0), templateCharArray, index, indexUpperBound),
				"messageLengthWithoutTermination")
			val messageLength =
				constructor.buildIntegerAddition(messageLengthWithoutTermination, constructor.buildInt32(1), "messageLength")
			val messageCharArray = constructor.buildHeapArrayAllocation(constructor.byteType, messageLength, "message")
			constructor.buildFunctionCall(context.externalFunctions.printToBuffer,
				listOf(messageCharArray, templateCharArray, index, indexUpperBound))
			val stringObject = constructor.buildFunctionCall(context.runtimeFunctions.createString,
				listOf(exceptionParameter, messageCharArray, messageLengthWithoutTermination), "messageString")
			context.raiseException(constructor, model, stringObject)
		} else {
			context.panic(constructor, outOfBoundsMessageTemplate, index, indexUpperBound)
			constructor.markAsUnreachable()
		}
		constructor.select(inBoundsBlock)
		return constructor.buildGetArrayElementPointer(elementType, thisArrayValue, index, "elementElement")
	}
}
