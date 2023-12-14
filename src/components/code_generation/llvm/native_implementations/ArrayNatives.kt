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

	private fun concatenate(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val arrayType = context.arrayTypeDeclaration?.llvmType
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(
			arrayType, thisObjectPointer,
			context.arrayValueIndex, "_propertyPointer")
		val thisArray = constructor.buildLoad(constructor.pointerType, thisPropertyPointer, "_thisArray")
		val thisSizePointer = context.resolveMember(constructor, arrayType, thisObjectPointer, "size")
		val thisSize = constructor.buildLoad(constructor.i32Type, thisSizePointer, "_thisSize")
		val parameterObjectPointer = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val parameterPropertyPointer = constructor.buildGetPropertyPointer(
			arrayType, parameterObjectPointer,
			context.arrayValueIndex, "_propertyPointer")
		val parameterArray = constructor.buildLoad(constructor.pointerType, parameterPropertyPointer, "_parameterArray")
		val parameterSizePointer = context.resolveMember(constructor, arrayType, parameterObjectPointer, "size")
		val parameterSize = constructor.buildLoad(constructor.i32Type, parameterSizePointer, "_parameterSize")
		val newArrayAddress = constructor.buildHeapAllocation(arrayType, "newArrayAddress")
		val arrayClassDefinitionPointer = constructor.buildGetPropertyPointer(arrayType, newArrayAddress,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "arrayClassDefinitionPointer")
		val arrayClassDefinitionAddress = context.arrayTypeDeclaration?.llvmClassDefinitionAddress
			?: throw CompilerError("Missing array type declaration.")
		constructor.buildStore(arrayClassDefinitionAddress, arrayClassDefinitionPointer)
		val newSizePointer = context.resolveMember(constructor, arrayType, newArrayAddress, "size")
		val combinedSize = constructor.buildIntegerAddition(thisSize, parameterSize, "_combinedSize")
		constructor.buildStore(combinedSize, newSizePointer)
		val newArrayPointer = constructor.buildHeapArrayAllocation(constructor.pointerType, combinedSize, "_newArray")
		constructor.buildFunctionCall(context.llvmMemoryCopyFunctionType, context.llvmMemoryCopyFunction,
			listOf(thisArray, newArrayPointer, thisSize), "_destination")
		val offsetNewArrayPointer = constructor.buildGetArrayElementPointer(constructor.pointerType, newArrayPointer, thisSize,
			"_offsetNewArrayPointer")
		constructor.buildFunctionCall(context.llvmMemoryCopyFunctionType, context.llvmMemoryCopyFunction,
			listOf(parameterArray, offsetNewArrayPointer, parameterSize), "_destination")
		val arrayValuePointer = constructor.buildGetPropertyPointer(arrayType, newArrayAddress, context.arrayValueIndex,
			"arrayValuePointer")
		constructor.buildStore(newArrayPointer, arrayValuePointer)
		constructor.buildReturn(newArrayAddress)
	}

	private fun get(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(
			context.arrayTypeDeclaration?.llvmType, thisObjectPointer,
			context.arrayValueIndex, "_propertyPointer")
		val indexValue = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val elementPointer = constructor.buildGetArrayElementPointer(constructor.pointerType, thisPropertyPointer, indexValue,
			"_arrayElementPointer")
		val result = constructor.buildLoad(constructor.pointerType, elementPointer, "_arrayElementValue")
		constructor.buildReturn(result)
	}

	private fun set(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(
			context.arrayTypeDeclaration?.llvmType, thisObjectPointer,
			context.arrayValueIndex, "_propertyPointer")
		val indexValue = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val valueValue = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET + 1)
		val elementPointer = constructor.buildGetArrayElementPointer(constructor.pointerType, thisPropertyPointer, indexValue,
			"_arrayElementPointer")
		constructor.buildStore(valueValue, elementPointer)
		constructor.buildReturn()
	}
}
