package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.NativeRegistry

class ArrayNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("<Element>Array(...Element)", ::fromPluralType)
//		registry.registerNativeImplementation("<Element>Array(<Element>Iterable)", ::fromIterable)
//		registry.registerNativeImplementation("<Element>Array(Element, Int)", ::fromValueToBeRepeated)
		registry.registerNativeImplementation("Array + <Element>Array: <Element>Array", ::concatenate)
		registry.registerNativeImplementation("Array[Int]: Element", ::get)
		registry.registerNativeImplementation("Array[Int](Element)", ::set)
	}

	private fun fromPluralType(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		val elementList = constructor.buildStackAllocation(context.variadicParameterListStruct, "elementList")
		constructor.buildFunctionCall(context.llvmVariableParameterIterationStartFunctionType,
			context.llvmVariableParameterIterationStartFunction, listOf(elementList))
		val elementCount = constructor.getLastParameter(llvmFunctionValue)
		val elementType = constructor.pointerType
		val arrayType = context.arrayDeclarationType
		val thisArray = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(arrayType, thisArray, context.arrayValueIndex,
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
		val element = constructor.buildCastFromIntegerToPointer(elementMemory, "element")
		val elementElement = constructor.buildGetArrayElementPointer(elementType, thisValue, index, "elementElement")
		constructor.buildStore(element, elementElement)
		val newIndex = constructor.buildIntegerAddition(index, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariable)
		constructor.buildJump(entryBlock)
		constructor.select(exitBlock)
		constructor.buildFunctionCall(context.llvmVariableParameterIterationEndFunctionType,
			context.llvmVariableParameterIterationEndFunction, listOf(elementList))
	}

	private fun fromIterable(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {

	}

	private fun fromValueToBeRepeated(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {

	}

	//TODO this function assumes all Arrays store pointers (see offset), but StringLiteral creates Arrays with native chars as elements
	private fun concatenate(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val arrayType = context.arrayDeclarationType
		val thisArray = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(arrayType, thisArray, context.arrayValueIndex,
			"thisValueProperty")
		val thisValue = constructor.buildLoad(constructor.pointerType, thisValueProperty, "thisValue")
		val thisSizeProperty = context.resolveMember(constructor, thisArray, "size")
		val thisSize = constructor.buildLoad(constructor.i32Type, thisSizeProperty, "thisSize")
		val parameterArray = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val parameterValueProperty = constructor.buildGetPropertyPointer(arrayType, parameterArray, context.arrayValueIndex,
			"parameterValueProperty")
		val parameterValue = constructor.buildLoad(constructor.pointerType, parameterValueProperty, "parameterValue")
		val parameterSizeProperty = context.resolveMember(constructor, parameterArray, "size")
		val parameterSize = constructor.buildLoad(constructor.i32Type, parameterSizeProperty, "parameterSize")
		val combinedArray = constructor.buildHeapAllocation(arrayType, "combinedArray")
		val combinedArrayClassDefinitionProperty = constructor.buildGetPropertyPointer(arrayType, combinedArray,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "combinedArrayClassDefinitionProperty")
		constructor.buildStore(context.arrayClassDefinition, combinedArrayClassDefinitionProperty)
		val combinedArraySizeProperty = context.resolveMember(constructor, combinedArray, "size")
		val combinedSize = constructor.buildIntegerAddition(thisSize, parameterSize, "combinedSize")
		constructor.buildStore(combinedSize, combinedArraySizeProperty)
		val combinedValue = constructor.buildHeapArrayAllocation(constructor.pointerType, combinedSize, "combinedValue")
		constructor.buildFunctionCall(context.llvmMemoryCopyFunctionType, context.llvmMemoryCopyFunction,
			listOf(thisValue, combinedValue, thisSize))
		val offsetCombinedArrayAddress = constructor.buildGetArrayElementPointer(constructor.pointerType, combinedValue, thisSize,
			"offsetCombinedArrayAddress")
		constructor.buildFunctionCall(context.llvmMemoryCopyFunctionType, context.llvmMemoryCopyFunction,
			listOf(parameterValue, offsetCombinedArrayAddress, parameterSize))
		val combinedArrayValueProperty = constructor.buildGetPropertyPointer(arrayType, combinedArray, context.arrayValueIndex,
			"combinedArrayValueProperty")
		constructor.buildStore(combinedValue, combinedArrayValueProperty)
		constructor.buildReturn(combinedArray)
	}

	private fun get(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisArray = context.getThisParameter(constructor)
		val thisArrayValueProperty = constructor.buildGetPropertyPointer(context.arrayDeclarationType, thisArray,
			context.arrayValueIndex, "thisArrayValueProperty")
		val index = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val element = constructor.buildGetArrayElementPointer(constructor.pointerType, thisArrayValueProperty, index, "element")
		val value = constructor.buildLoad(constructor.pointerType, element, "value")
		constructor.buildReturn(value)
	}

	private fun set(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisArray = context.getThisParameter(constructor)
		val thisArrayValueProperty = constructor.buildGetPropertyPointer(context.arrayDeclarationType, thisArray,
			context.arrayValueIndex, "thisArrayValueProperty")
		val index = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val value = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET + 1)
		val element = constructor.buildGetArrayElementPointer(constructor.pointerType, thisArrayValueProperty, index, "element")
		constructor.buildStore(value, element)
		constructor.buildReturn()
	}
}
