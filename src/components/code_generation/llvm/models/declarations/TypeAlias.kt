package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.declarations.TypeAlias
import errors.internal.CompilerError
import java.util.*

class TypeAlias(override val model: TypeAlias, val instances: List<Instance>): TypeDeclaration(model, instances) {

	override fun define(constructor: LlvmConstructor) {
		super.declare(constructor)
		val llvmType = model.finalEffectiveType.getLlvmType(constructor)
		for(instance in instances) {
			instance.llvmLocation = constructor.declareGlobal("${model.name}_${instance.model.name}_TypeAliasInstance", llvmType)
			constructor.defineGlobal(instance.llvmLocation, context.getNullValue(constructor, model.finalEffectiveType))
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		val exceptionAddress = context.getExceptionParameter(constructor)
		for(instance in instances) {
			val value = getInstanceValue(constructor, exceptionAddress, instance)
			constructor.buildStore(value, instance.llvmLocation)
		}
	}

	private fun getInstanceValue(constructor: LlvmConstructor, exceptionAddress: LlvmValue, instance: Instance): LlvmValue {
		val initializerModel =
			instance.model.initializer ?: throw CompilerError(model, "Missing initializer in type alias instance declaration.")
		val typeDeclarationModel = initializerModel.parentTypeDeclaration
		val parameters = LinkedList<LlvmValue?>()
		for((index, valueParameter) in instance.valueParameters.withIndex())
			parameters.add(ValueConverter.convertIfRequired(model, constructor, valueParameter.getLlvmValue(constructor),
				valueParameter.model.providedType, initializerModel.getParameterTypeAt(index),
				instance.model.conversions?.get(valueParameter.model)))
		if(initializerModel.isVariadic) {
			val fixedParameterCount = initializerModel.fixedParameters.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		if(initializerModel.parentTypeDeclaration.isLlvmPrimitive()) {
			val signature = initializerModel.toString()
			if(initializerModel.isNative)
				return context.nativeRegistry.inlineNativePrimitiveInitializer(constructor, "${signature}: Self", parameters)
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			return constructor.buildFunctionCall(initializerModel.unit.llvmType, initializerModel.unit.llvmValue, parameters, signature)
		}
		val instanceValue =
			constructor.buildHeapAllocation(typeDeclarationModel.unit.llvmType, "${typeDeclarationModel.name}_${model.name}_Instance")
		buildLlvmCommonPreInitializerCall(constructor, typeDeclarationModel.unit, exceptionAddress, instanceValue)
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, instanceValue)
		constructor.buildFunctionCall(initializerModel.unit.llvmType, initializerModel.unit.llvmValue, parameters)
		context.continueRaise(constructor, model)
		return instanceValue
	}

	private fun buildLlvmCommonPreInitializerCall(constructor: LlvmConstructor, typeDeclaration: TypeDeclaration,
												  exceptionAddress: LlvmValue, newObject: LlvmValue) {
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
		constructor.buildFunctionCall(typeDeclaration.commonClassPreInitializer, parameters)
		context.continueRaise(constructor, model)
	}
}
