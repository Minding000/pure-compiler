package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.NativeRegistry

class ByteArrayNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("ByteArray(...Byte)", ::fromPluralType)
		registry.registerNativeImplementation("ByteArray(Byte, Int)", ::fromValueToBeRepeated)
		registry.registerNativeImplementation("ByteArray + ByteArray: ByteArray", ::concatenate)
		registry.registerNativeImplementation("ByteArray[Int]: Byte", ::get)
		registry.registerNativeImplementation("ByteArray[Int](Byte)", ::set)
	}

	private fun fromPluralType(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.byteType
		val elementList = constructor.buildStackAllocation(context.variadicParameterListStruct, "elementList")
		constructor.buildFunctionCall(context.llvmVariableParameterIterationStartFunctionType,
			context.llvmVariableParameterIterationStartFunction, listOf(elementList))
		val elementCount = constructor.getLastParameter(llvmFunctionValue)
		val arrayType = context.byteArrayDeclarationType
		val thisArray = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(arrayType, thisArray, context.byteArrayValueIndex,
			"thisValueProperty")
		val thisValue = constructor.buildHeapArrayAllocation(elementType, elementCount, "thisValue")
		constructor.buildStore(thisValue, thisValueProperty)
		val thisSizeProperty = context.resolveMember(constructor, thisArray, "size")
		constructor.buildStore(elementCount, thisSizeProperty)
		val indexType = constructor.i32Type
		val indexVariable = constructor.buildStackAllocation(indexType, "indexVariable")
		constructor.buildStore(constructor.buildInt32(0), indexVariable)
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
		val element = constructor.buildCastFromIntegerToByte(elementMemory, "element")
		val elementElement = constructor.buildGetArrayElementPointer(elementType, thisValue, index, "elementElement")
		constructor.buildStore(element, elementElement)
		val newIndex = constructor.buildIntegerAddition(index, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariable)
		constructor.buildJump(entryBlock)
		constructor.select(exitBlock)
		constructor.buildFunctionCall(context.llvmVariableParameterIterationEndFunctionType,
			context.llvmVariableParameterIterationEndFunction, listOf(elementList))
	}

	private fun fromValueToBeRepeated(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.byteType
		val element = constructor.getParameter(Context.VALUE_PARAMETER_OFFSET)
		val elementCount = constructor.getLastParameter(llvmFunctionValue)
		val arrayType = context.byteArrayDeclarationType
		val thisArray = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(arrayType, thisArray, context.byteArrayValueIndex,
			"thisValueProperty")
		val thisValue = constructor.buildHeapArrayAllocation(elementType, elementCount, "thisValue")
		constructor.buildStore(thisValue, thisValueProperty)
		val indexType = constructor.i32Type
		val indexVariable = constructor.buildStackAllocation(indexType, "indexVariable")
		constructor.buildStore(constructor.buildInt32(0), indexVariable)
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

	private fun concatenate(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.byteType
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val arrayType = context.byteArrayDeclarationType
		val thisArray = context.getThisParameter(constructor)
		val thisArrayValueProperty = constructor.buildGetPropertyPointer(arrayType, thisArray, context.byteArrayValueIndex,
			"thisArrayValueProperty")
		val thisArrayValue = constructor.buildLoad(constructor.pointerType, thisArrayValueProperty, "thisArrayValue")
		val thisSizeProperty = context.resolveMember(constructor, thisArray, "size")
		val thisSize = constructor.buildLoad(constructor.i32Type, thisSizeProperty, "thisSize")
		val parameterArray = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val parameterValueProperty = constructor.buildGetPropertyPointer(arrayType, parameterArray, context.byteArrayValueIndex,
			"parameterValueProperty")
		val parameterValue = constructor.buildLoad(constructor.pointerType, parameterValueProperty, "parameterValue")
		val parameterSizeProperty = context.resolveMember(constructor, parameterArray, "size")
		val parameterSize = constructor.buildLoad(constructor.i32Type, parameterSizeProperty, "parameterSize")
		val combinedArray = constructor.buildHeapAllocation(arrayType, "combinedArray")
		val combinedArrayClassDefinitionProperty = constructor.buildGetPropertyPointer(arrayType, combinedArray,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "combinedArrayClassDefinitionProperty")
		constructor.buildStore(context.byteArrayClassDefinition, combinedArrayClassDefinitionProperty)
		val combinedArraySizeProperty = context.resolveMember(constructor, combinedArray, "size")
		val combinedSize = constructor.buildIntegerAddition(thisSize, parameterSize, "combinedSize")
		constructor.buildStore(combinedSize, combinedArraySizeProperty)
		val combinedValue = constructor.buildHeapArrayAllocation(elementType, combinedSize, "combinedValue")
		constructor.buildFunctionCall(context.llvmMemoryCopyFunctionType, context.llvmMemoryCopyFunction,
			listOf(thisArrayValue, combinedValue, thisSize))
		val offsetCombinedArrayAddress = constructor.buildGetArrayElementPointer(elementType, combinedValue, thisSize,
			"offsetCombinedArrayAddress")
		constructor.buildFunctionCall(context.llvmMemoryCopyFunctionType, context.llvmMemoryCopyFunction,
			listOf(parameterValue, offsetCombinedArrayAddress, parameterSize))
		val combinedArrayValueProperty = constructor.buildGetPropertyPointer(arrayType, combinedArray, context.byteArrayValueIndex,
			"combinedArrayValueProperty")
		constructor.buildStore(combinedValue, combinedArrayValueProperty)
		constructor.buildReturn(combinedArray)
	}

	private fun get(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.byteType
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisArray = context.getThisParameter(constructor)
		val thisArrayValueProperty = constructor.buildGetPropertyPointer(context.byteArrayDeclarationType, thisArray,
			context.byteArrayValueIndex, "thisArrayValueProperty")
		val thisArrayValue = constructor.buildLoad(constructor.pointerType, thisArrayValueProperty, "thisArrayValue")
		val index = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val elementElement = constructor.buildGetArrayElementPointer(elementType, thisArrayValue, index, "elementElement")
		val element = constructor.buildLoad(elementType, elementElement, "value")
		constructor.buildReturn(element)
	}

	private fun set(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.byteType
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisArray = context.getThisParameter(constructor)
		val thisArrayValueProperty = constructor.buildGetPropertyPointer(context.byteArrayDeclarationType, thisArray,
			context.byteArrayValueIndex, "thisArrayValueProperty")
		val index = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val element = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET + 1)
		val elementElement = constructor.buildGetArrayElementPointer(elementType, thisArrayValueProperty, index, "elementElement")
		constructor.buildStore(element, elementElement)
		constructor.buildReturn()
	}
}
