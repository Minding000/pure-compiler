package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import errors.internal.CompilerError

object ArrayNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
		context.registerNativeImplementation("Array + <Element>Array: <Element>Array", ::concatenate)
		context.registerNativeImplementation("Array[Int]: Element", ::get)
		context.registerNativeImplementation("Array[Int](Element)", ::set)
	}

	//TODO this function assumes all Arrays store pointers (see offset), but StringLiteral creates Arrays with native chars as elements
	private fun concatenate(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val arrayType = context.arrayTypeDeclaration?.llvmType
		val thisArray = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(arrayType, thisArray, context.arrayValueIndex,
			"thisValueProperty")
		val thisValue = constructor.buildLoad(constructor.pointerType, thisValueProperty, "thisValue")
		val thisSizeProperty = context.resolveMember(constructor, arrayType, thisArray, "size")
		val thisSize = constructor.buildLoad(constructor.i32Type, thisSizeProperty, "thisSize")
		val parameterArray = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val parameterValueProperty = constructor.buildGetPropertyPointer(arrayType, parameterArray, context.arrayValueIndex,
			"parameterValueProperty")
		val parameterValue = constructor.buildLoad(constructor.pointerType, parameterValueProperty, "parameterValue")
		val parameterSizeProperty = context.resolveMember(constructor, arrayType, parameterArray, "size")
		val parameterSize = constructor.buildLoad(constructor.i32Type, parameterSizeProperty, "parameterSize")
		val combinedArray = constructor.buildHeapAllocation(arrayType, "combinedArray")
		val combinedArrayClassDefinitionProperty = constructor.buildGetPropertyPointer(arrayType, combinedArray,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "combinedArrayClassDefinitionProperty")
		val arrayClassDefinition = context.arrayTypeDeclaration?.llvmClassDefinition
			?: throw CompilerError("Missing array type declaration.")
		constructor.buildStore(arrayClassDefinition, combinedArrayClassDefinitionProperty)
		val combinedArraySizeProperty = context.resolveMember(constructor, arrayType, combinedArray, "size")
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
		val thisArrayValueProperty = constructor.buildGetPropertyPointer(context.arrayTypeDeclaration?.llvmType, thisArray,
			context.arrayValueIndex, "thisArrayValueProperty")
		val index = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val element = constructor.buildGetArrayElementPointer(constructor.pointerType, thisArrayValueProperty, index, "element")
		val value = constructor.buildLoad(constructor.pointerType, element, "value")
		constructor.buildReturn(value)
	}

	private fun set(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisArray = context.getThisParameter(constructor)
		val thisArrayValueProperty = constructor.buildGetPropertyPointer(context.arrayTypeDeclaration?.llvmType, thisArray,
			context.arrayValueIndex, "thisArrayValueProperty")
		val index = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val value = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET + 1)
		val element = constructor.buildGetArrayElementPointer(constructor.pointerType, thisArrayValueProperty, index, "element")
		constructor.buildStore(value, element)
		constructor.buildReturn()
	}
}
