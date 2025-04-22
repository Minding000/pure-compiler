package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.declarations.Instance
import errors.internal.CompilerError
import java.util.*

class Instance(override val model: Instance, val valueParameters: List<Value>): ValueDeclaration(model, null, valueParameters) {

	override fun requiresFileRunner(): Boolean {
		//TODO What about isNative?
		return !model.isAbstract
	}

	fun getLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val initializer = model.initializer ?: throw CompilerError(model, "Missing initializer in instance declaration.")
		val exceptionAddress = context.getExceptionParameter(constructor)
		val parameters = LinkedList<LlvmValue?>()
		for((index, valueParameter) in valueParameters.withIndex())
			parameters.add(ValueConverter.convertIfRequired(model, constructor, valueParameter.getLlvmValue(constructor),
				valueParameter.model.providedType, initializer.getParameterTypeAt(index), model.conversions?.get(valueParameter.model)))
		if(initializer.isVariadic) {
			val fixedParameterCount = initializer.fixedParameters.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		if(initializer.parentTypeDeclaration.isLlvmPrimitive()) {
			val signature = initializer.toString()
			if(initializer.isNative)
				return context.nativeRegistry.inlineNativePrimitiveInitializer(model, constructor, "$signature: Self", parameters)
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			return constructor.buildFunctionCall(initializer.unit.llvmType, initializer.unit.llvmValue, parameters, signature)
		}
		val typeDeclaration = initializer.parentTypeDeclaration.unit
		val instance = constructor.buildHeapAllocation(typeDeclaration.llvmType, "${typeDeclaration.model.name}_${model.name}_Instance")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(typeDeclaration.llvmType, instance,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
		constructor.buildStore(typeDeclaration.llvmClassDefinition, classDefinitionProperty)
		buildLlvmCommonPreInitializerCall(constructor, typeDeclaration, exceptionAddress, instance)
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, instance)
		constructor.buildFunctionCall(initializer.unit.llvmType, initializer.unit.llvmValue, parameters)
		context.continueRaise(constructor, model)
		return instance
	}

	private fun buildLlvmCommonPreInitializerCall(constructor: LlvmConstructor,
												  typeDeclaration: components.code_generation.llvm.models.declarations.TypeDeclaration,
												  exceptionAddress: LlvmValue, newObject: LlvmValue) {
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
		constructor.buildFunctionCall(typeDeclaration.commonClassPreInitializer, parameters)
		context.continueRaise(constructor, model)
	}
}
