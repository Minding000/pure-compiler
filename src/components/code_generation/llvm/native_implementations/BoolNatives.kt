package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context

object BoolNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
		context.registerNativeImplementation("Bool!: Bool", ::negate)
		context.registerNativeImplementation("Bool and Bool: Bool", ::and)
		context.registerNativeImplementation("Bool or Bool: Bool", ::or)
		context.registerNativeImplementation("Bool.toggle()", ::toggle)
	}

	private fun unwrap(constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val thisPropertyPointer = constructor.buildGetPropertyPointer(
			context.booleanTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.booleanValueIndex, "_thisPropertyPointer")
		return constructor.buildLoad(constructor.booleanType, thisPropertyPointer, "_thisPrimitiveValue")
	}

	private fun negate(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val result = constructor.buildBooleanNegation(thisPrimitiveValue, "_negationResult")
		constructor.buildReturn(result)
	}

	private fun and(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterValue = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildAnd(thisPrimitiveValue, parameterValue, "_andResult")
		constructor.buildReturn(result)
	}

	private fun or(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisPrimitiveValue = unwrap(constructor, context.getThisParameter(constructor))
		val parameterValue = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildOr(thisPrimitiveValue, parameterValue, "_orResult")
		constructor.buildReturn(result)
	}

	private fun toggle(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisObjectPointer = context.getThisParameter(constructor)
		val thisPropertyPointer = constructor.buildGetPropertyPointer(
			context.booleanTypeDeclaration?.llvmType, thisObjectPointer,
			context.booleanValueIndex, "_thisPropertyPointer")
		val thisPrimitiveValue = constructor.buildLoad(constructor.booleanType, thisPropertyPointer, "_thisPrimitiveValue")
		val result = constructor.buildBooleanNegation(thisPrimitiveValue, "_negationResult")
		constructor.buildStore(result, thisPropertyPointer)
		constructor.buildReturn()
	}
}
