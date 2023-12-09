package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import errors.internal.CompilerError

object IntNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
		context.nativeImplementations["Int + Self: Self"] = ::plus
	}

	private fun plus(constructor: LlvmConstructor, llvmValue: LlvmValue) {
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectBlock(llvmValue, "entrypoint")
		val thisValue = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, thisValue,
			context.integerValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.i32Type, thisPropertyPointer, "_thisPrimitiveValue")
		val parameterValue = constructor.getParameter(llvmValue, Context.VALUE_PARAMETER_OFFSET)
		val parameterPropertyPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, parameterValue,
			context.integerValueIndex, "_parameterPropertyPointer")
		val parameterPrimitiveValue = constructor.buildLoad(constructor.i32Type, parameterPropertyPointer, "_parameterPrimitiveValue")
		val result = constructor.buildIntegerAddition(thisPrimitiveValue, parameterPrimitiveValue, "_additionResult")
		val newIntegerAddress = constructor.buildHeapAllocation(context.integerTypeDeclaration?.llvmType, "newIntegerAddress")
		val integerClassDefinitionPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType,
			newIntegerAddress, Context.CLASS_DEFINITION_PROPERTY_INDEX, "integerClassDefinitionPointer")
		val integerClassDefinitionAddress = context.integerTypeDeclaration?.llvmClassDefinitionAddress
			?: throw CompilerError("Missing integer type declaration.")
		constructor.buildStore(integerClassDefinitionAddress, integerClassDefinitionPointer)
		val valuePointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, newIntegerAddress,
			context.integerValueIndex, "valuePointer")
		constructor.buildStore(result, valuePointer)
		constructor.buildReturn(valuePointer)
		constructor.select(previousBlock)
	}
}
