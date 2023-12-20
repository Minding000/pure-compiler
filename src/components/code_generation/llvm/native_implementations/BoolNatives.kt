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
		val boolValueProperty = constructor.buildGetPropertyPointer(context.booleanTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.booleanValueIndex, "_valueProperty")
		return constructor.buildLoad(constructor.booleanType, boolValueProperty, "_value")
	}

	private fun negate(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveBool = unwrap(constructor, context.getThisParameter(constructor))
		val result = constructor.buildBooleanNegation(thisPrimitiveBool, "negationResult")
		constructor.buildReturn(result)
	}

	private fun and(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveBool = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveBool = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildAnd(thisPrimitiveBool, parameterPrimitiveBool, "andResult")
		constructor.buildReturn(result)
	}

	private fun or(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisPrimitiveBool = unwrap(constructor, context.getThisParameter(constructor))
		val parameterPrimitiveBool = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildOr(thisPrimitiveBool, parameterPrimitiveBool, "orResult")
		constructor.buildReturn(result)
	}

	private fun toggle(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisBool = context.getThisParameter(constructor)
		val thisValueProperty = constructor.buildGetPropertyPointer(context.booleanTypeDeclaration?.llvmType, thisBool,
			context.booleanValueIndex, "thisValueProperty")
		val thisPrimitiveBool = constructor.buildLoad(constructor.booleanType, thisValueProperty, "thisPrimitiveBool")
		val result = constructor.buildBooleanNegation(thisPrimitiveBool, "negationResult")
		constructor.buildStore(result, thisValueProperty)
		constructor.buildReturn()
	}
}
