package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context

class ByteArrayNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("ByteArray(...Byte)", ::fromPluralType)
		registry.registerNativeImplementation("ByteArray(Byte, Int)", ::fromValueToBeRepeated)
		registry.registerNativeImplementation("ByteArray + ByteArray: ByteArray", ::concatenate)
		registry.registerNativeImplementation("ByteArray[Int]: Byte", ::get)
		registry.registerNativeImplementation("ByteArray[Int](Byte)", ::set)
		registry.registerNativeImplementation("ByteArray == Any?: Bool", ::equalTo)
	}

	private fun fromPluralType(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.byteType
		val elementList = constructor.buildStackAllocation(context.runtimeStructs.variadicParameterList, "elementList")
		constructor.buildFunctionCall(context.externalFunctions.variableParameterIterationStart, listOf(elementList))
		val elementCount = constructor.getLastParameter(llvmFunctionValue)
		val thisByteArray = context.getThisParameter(constructor)
		val thisValueProperty = context.standardLibrary.byteArray.getNativeValueProperty(constructor, thisByteArray)
		val thisValue = constructor.buildHeapArrayAllocation(elementType, elementCount, "thisValue")
		constructor.buildStore(thisValue, thisValueProperty)
		val thisSizeProperty = context.resolveMember(constructor, thisByteArray, "size")
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
		constructor.buildFunctionCall(context.externalFunctions.variableParameterIterationEnd, listOf(elementList))
	}

	private fun fromValueToBeRepeated(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.byteType
		val element = constructor.getParameter(Context.VALUE_PARAMETER_OFFSET)
		val elementCount = constructor.getLastParameter(llvmFunctionValue)
		val thisValueProperty = context.standardLibrary.byteArray.getNativeValueProperty(constructor, context.getThisParameter(constructor))
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
		val runtimeClass = context.standardLibrary.byteArray
		val thisArray = context.getThisParameter(constructor)
		val thisArrayValueProperty = runtimeClass.getNativeValueProperty(constructor, thisArray)
		val thisArrayValue = constructor.buildLoad(constructor.pointerType, thisArrayValueProperty, "thisArrayValue")
		val thisSizeProperty = context.resolveMember(constructor, thisArray, "size")
		val thisSize = constructor.buildLoad(constructor.i32Type, thisSizeProperty, "thisSize")
		val thisSizeAsLong = constructor.buildCastFromIntegerToLong(thisSize, "thisSizeAsLong")
		val parameterArray = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val parameterValueProperty = runtimeClass.getNativeValueProperty(constructor, parameterArray)
		val parameterValue = constructor.buildLoad(constructor.pointerType, parameterValueProperty, "parameterValue")
		val parameterSizeProperty = context.resolveMember(constructor, parameterArray, "size")
		val parameterSize = constructor.buildLoad(constructor.i32Type, parameterSizeProperty, "parameterSize")
		val parameterSizeAsLong = constructor.buildCastFromIntegerToLong(parameterSize, "parameterSizeAsLong")
		val combinedArray = constructor.buildHeapAllocation(runtimeClass.struct, "combinedArray")
		runtimeClass.setClassDefinition(constructor, combinedArray)
		val combinedArraySizeProperty = context.resolveMember(constructor, combinedArray, "size")
		val combinedSize = constructor.buildIntegerAddition(thisSize, parameterSize, "combinedSize")
		constructor.buildStore(combinedSize, combinedArraySizeProperty)
		val combinedValue = constructor.buildHeapArrayAllocation(elementType, combinedSize, "combinedValue")
		constructor.buildFunctionCall(context.externalFunctions.memoryCopy, listOf(combinedValue, thisArrayValue, thisSizeAsLong))
		val offsetCombinedArrayAddress = constructor.buildGetArrayElementPointer(elementType, combinedValue, thisSize,
			"offsetCombinedArrayAddress")
		constructor.buildFunctionCall(context.externalFunctions.memoryCopy,
			listOf(offsetCombinedArrayAddress, parameterValue, parameterSizeAsLong))
		val combinedArrayValueProperty = runtimeClass.getNativeValueProperty(constructor, combinedArray)
		constructor.buildStore(combinedValue, combinedArrayValueProperty)
		constructor.buildReturn(combinedArray)
	}

	private fun get(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.byteType
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisArrayValueProperty =
			context.standardLibrary.byteArray.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisArrayValue = constructor.buildLoad(constructor.pointerType, thisArrayValueProperty, "thisArrayValue")
		val index = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val elementElement = constructor.buildGetArrayElementPointer(elementType, thisArrayValue, index, "elementElement")
		val element = constructor.buildLoad(elementType, elementElement, "value")
		constructor.buildReturn(element)
	}

	private fun set(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementType = constructor.byteType
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisArrayValueProperty =
			context.standardLibrary.byteArray.getNativeValueProperty(constructor, context.getThisParameter(constructor))
		val thisArrayValue = constructor.buildLoad(constructor.pointerType, thisArrayValueProperty, "thisArrayValue")
		val index = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val element = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET + 1)
		val elementElement = constructor.buildGetArrayElementPointer(elementType, thisArrayValue, index, "elementElement")
		constructor.buildStore(element, elementElement)
		constructor.buildReturn()
	}

	private fun equalTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		// check type
		val runtimeClass = context.standardLibrary.byteArray
		val other = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val byteArrayBlock = constructor.createBlock("byteArray")
		val notByteArrayBlock = constructor.createBlock("notByteArray")
		val isOtherByteArray =
			constructor.buildPointerEqualTo(context.getClassDefinition(constructor, other), runtimeClass.classDefinition,
				"isOtherByteArray")
		constructor.buildJump(isOtherByteArray, byteArrayBlock, notByteArrayBlock)
		constructor.select(notByteArrayBlock)
		constructor.buildReturn(constructor.buildBoolean(false))
		constructor.select(byteArrayBlock)
		// compare size
		val thisByteArray = context.getThisParameter(constructor)
		val thisSizeProperty = context.resolveMember(constructor, thisByteArray, "size")
		val thisSize = constructor.buildLoad(constructor.i32Type, thisSizeProperty, "thisSize")
		val otherProperty = context.resolveMember(constructor, other, "size")
		val otherSize = constructor.buildLoad(constructor.i32Type, otherProperty, "otherSize")
		val sameSizeBlock = constructor.createBlock("sameSize")
		val differentSizeBlock = constructor.createBlock("differentSize")
		val hasSameSize = constructor.buildSignedIntegerEqualTo(otherSize, thisSize, "hasSameSize")
		constructor.buildJump(hasSameSize, sameSizeBlock, differentSizeBlock)
		constructor.select(differentSizeBlock)
		constructor.buildReturn(constructor.buildBoolean(false))
		constructor.select(sameSizeBlock)
		// compare bytes
		val thisValueProperty = runtimeClass.getNativeValueProperty(constructor, thisByteArray)
		val otherValueProperty = runtimeClass.getNativeValueProperty(constructor, other)
		val thisValue = constructor.buildLoad(constructor.pointerType, thisValueProperty, "thisValue")
		val otherValue = constructor.buildLoad(constructor.pointerType, otherValueProperty, "otherValue")
		val loopStartBlock = constructor.createBlock("loopStart")
		val loopBodyBlock = constructor.createBlock("loopBody")
		val sameBytesBlock = constructor.createBlock("sameBytes")
		val differentByteBlock = constructor.createBlock("differentByte")
		val indexVariable = constructor.buildStackAllocation(constructor.i32Type, "indexVariable")
		constructor.buildStore(constructor.buildInt32(0), indexVariable)
		constructor.buildJump(loopStartBlock)
		constructor.select(loopStartBlock)
		val index = constructor.buildLoad(constructor.i32Type, indexVariable, "index")
		val isDone = constructor.buildSignedIntegerGreaterThanOrEqualTo(index, thisSize, "isDone")
		constructor.buildJump(isDone, sameBytesBlock, loopBodyBlock)
		constructor.select(loopBodyBlock)
		val thisByteLocation = constructor.buildGetArrayElementPointer(constructor.byteType, thisValue, index, "thisByteLocation")
		val otherByteLocation = constructor.buildGetArrayElementPointer(constructor.byteType, otherValue, index, "otherByteLocation")
		val thisByte = constructor.buildLoad(constructor.byteType, thisByteLocation, "thisByte")
		val otherByte = constructor.buildLoad(constructor.byteType, otherByteLocation, "otherByte")
		val areBytesEqual = constructor.buildSignedIntegerEqualTo(thisByte, otherByte, "areBytesEqual")
		val newIndex = constructor.buildIntegerAddition(index, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariable)
		constructor.buildJump(areBytesEqual, loopStartBlock, differentByteBlock)
		constructor.select(sameBytesBlock)
		constructor.buildReturn(constructor.buildBoolean(true))
		constructor.select(differentByteBlock)
		constructor.buildReturn(constructor.buildBoolean(false))
	}
}
