package components.code_generation.llvm.models.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.operations.IndexAccess
import errors.internal.CompilerError
import java.util.*

class IndexAccess(override val model: IndexAccess, val target: Value, val indices: List<Value>):
	Value(model, listOf(target, *indices.toTypedArray())) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val signature = model.targetSignature?.original ?: throw CompilerError(this, "Index access is missing a target.")
		return createLlvmGetterCall(constructor, signature)
	}

	private fun createLlvmGetterCall(constructor: LlvmConstructor, signature: FunctionSignature): LlvmValue {
		val targetValue = target.getLlvmValue(constructor) //TODO convert (write test)
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(targetValue)
		for((indexIndex, indexValue) in indices.withIndex()) {
			val parameterType = signature.getParameterTypeAt(indexIndex)
			parameters.add(
				ValueConverter.convertIfRequired(model, constructor, indexValue.getLlvmValue(constructor), indexValue.model.effectiveType,
					indexValue.model.hasGenericType, parameterType, parameterType != signature.original.getParameterTypeAt(indexIndex)))
		}
		val functionAddress = context.resolveFunction(constructor, targetValue, signature.getIdentifier(model.getOperatorKind()))
		val returnValue = constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters,
			"_indexAccess_result")
		context.continueRaise(constructor, model)
		return returnValue
	}
}
