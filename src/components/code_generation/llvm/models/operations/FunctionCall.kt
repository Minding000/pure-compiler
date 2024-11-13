package components.code_generation.llvm.models.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.values.SuperReference
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.models.values.VariableValue
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.operations.FunctionCall
import components.semantic_model.operations.UnaryOperator
import components.semantic_model.types.ObjectType
import components.semantic_model.values.Operator
import errors.internal.CompilerError
import java.util.*

class FunctionCall(override val model: FunctionCall, val function: Value, val valueParameters: List<Value> = emptyList()):
	Value(model, listOf(function, *valueParameters.toTypedArray())) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val exceptionAddress = context.getExceptionParameter(constructor)
		val functionSignature = model.targetSignature
		val returnValue = if(functionSignature == null) {
			val initializerDefinition = model.targetInitializer ?: throw CompilerError(this, "Function call is missing a target.")
			buildLlvmInitializerCall(constructor, initializerDefinition, exceptionAddress)
		} else {
			buildLlvmFunctionCall(constructor, functionSignature, exceptionAddress)
		}
		return returnValue
	}

	private fun buildLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature, exceptionAddress: LlvmValue): LlvmValue {
		val parameters = LinkedList<LlvmValue>()
		//TODO add local type parameters
		for((index, valueParameter) in valueParameters.withIndex()) {
			val valueParameterModel = valueParameter.model
			if(valueParameterModel is UnaryOperator && valueParameterModel.kind == Operator.Kind.TRIPLE_DOT) {
				TODO("The spread operator is not implemented yet.")
				//TODO scrap va_lists, because they require a static parameter count
			} else {
				//TODO detect class-generic parameter type
				val parameterType = model.targetSignature?.getParameterTypeAt(index)?.effectiveType
				//TODO isTargetGeneric: check: if parameter has SelfType, function is overridden and type is primitive add an extraneous conversion (same for operators)
				parameters.add(ValueConverter.convertIfRequired(model, constructor, valueParameter.getLlvmValue(constructor),
					valueParameter.model.effectiveType, valueParameter.model.hasGenericType, parameterType,
					parameterType != model.targetSignature?.original?.getParameterTypeAt(index),
					model.conversions?.get(valueParameter.model)))
			}
		}
		if(signature.isVariadic) {
			val fixedParameterCount = signature.fixedParameterTypes.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		val typeDefinition = signature.parentTypeDeclaration
		val functionAddress = if(typeDefinition == null) {
			val implementation = signature.associatedImplementation
			if(implementation == null) {
				//TODO add captured variables as parameters
				val closureLocation = function.getLlvmValue(constructor)
				constructor.buildGetPropertyPointer(context.runtimeStructs.closure, closureLocation,
					Context.CLOSURE_FUNCTION_ADDRESS_PROPERTY_INDEX, "_functionAddress")
			} else {
				implementation.unit.llvmValue
			}
		} else if(typeDefinition.isLlvmPrimitive()) {
			//TODO same for operators (write test!)
			//TODO does this work for optional primitives? (write test!) (same for: operators, getters)
			val targetValue = if(function is MemberAccess)
				function.target.getLlvmValue(constructor)
			else
				context.getThisParameter(constructor)
			val functionName = (((function as? MemberAccess)?.member ?: function) as? VariableValue)?.model?.name
				?: throw CompilerError(this, "Failed to determine name of member function.")
			val primitiveImplementation = context.nativeRegistry.resolvePrimitiveImplementation(
				"${typeDefinition.name}.${functionName}${signature.original.toString(false)}")
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			parameters.add(Context.THIS_PARAMETER_INDEX, targetValue)
			val resultName = if(SpecialType.NOTHING.matches(signature.returnType)) "" else model.getSignature()
			val result = constructor.buildFunctionCall(primitiveImplementation.llvmType, primitiveImplementation.llvmValue, parameters,
				resultName)
			context.continueRaise(constructor, model)
			return result
		} else {
			val targetValue = if(function is MemberAccess)
				function.target.getLlvmValue(constructor)
			else
				context.getThisParameter(constructor)
			parameters.addFirst(targetValue)
			if(function is MemberAccess && function.target is SuperReference) {
				val implementation = signature.associatedImplementation
					?: throw CompilerError(this, "Encountered member signature without implementation.")
				implementation.unit.llvmValue
			} else {
				val functionName = (((function as? MemberAccess)?.member ?: function) as? VariableValue)?.model?.name
					?: throw CompilerError(this, "Failed to determine name of member function.")
				context.resolveFunction(constructor, targetValue, signature.getIdentifier(functionName))
			}
		}
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		val resultName = if(SpecialType.NOTHING.matches(signature.returnType)) "" else model.getSignature()
		val result = constructor.buildFunctionCall(signature.original.getLlvmType(constructor), functionAddress, parameters, resultName)
		context.continueRaise(constructor, model)
		return result
	}

	private fun buildLlvmInitializerCall(constructor: LlvmConstructor, initializerModel: InitializerDefinition,
										 exceptionAddress: LlvmValue): LlvmValue {
		val parameters = LinkedList<LlvmValue?>()
		//TODO add local type parameters
		for((index, valueParameter) in valueParameters.withIndex()) {
			//TODO detect class-generic parameter type
			val parameterType = initializerModel.getParameterTypeAt(index)?.effectiveType
			parameters.add(ValueConverter.convertIfRequired(model, constructor, valueParameter.getLlvmValue(constructor),
				valueParameter.model.effectiveType, valueParameter.model.hasGenericType, parameterType, false,
				model.conversions?.get(valueParameter.model)))
		}
		if(initializerModel.isVariadic) {
			val fixedParameterCount = initializerModel.fixedParameters.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		//TODO primary initializer calls should also be resolved dynamically (for generic type initialization)
		// - unless the variable definition is a specific type already (or can be traced to it)
		return if(model.isPrimaryCall) {
			if(initializerModel.parentTypeDeclaration.isLlvmPrimitive()) {
				val signature = initializerModel.toString()
				if(initializerModel.isNative)
					return context.nativeRegistry.inlineNativePrimitiveInitializer(constructor, "$signature: Self", parameters)
				parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
				val result =
					constructor.buildFunctionCall(initializerModel.unit.llvmType, initializerModel.unit.llvmValue, parameters, signature)
				context.continueRaise(constructor, model)
				return result
			}
			val typeDeclaration = initializerModel.parentTypeDeclaration.unit
			val newObject = constructor.buildHeapAllocation(typeDeclaration.llvmType, "newObject")
			val classDefinitionProperty = constructor.buildGetPropertyPointer(typeDeclaration.llvmType, newObject,
				Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
			constructor.buildStore(typeDeclaration.llvmClassDefinition, classDefinitionProperty)
			buildLlvmCommonPreInitializerCall(constructor, typeDeclaration, exceptionAddress, newObject)
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
			constructor.buildFunctionCall(initializerModel.unit.llvmType, initializerModel.unit.llvmValue, parameters)
			context.continueRaise(constructor, model)
			newObject
		} else if(initializerModel.parentTypeDeclaration.isLlvmPrimitive()) {
			if(parameters.size != 1)
				throw CompilerError("Invalid number of arguments passed to '${model.getSignature()}': ${parameters.size}")
			val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '${model.getSignature()}' is null.")
			constructor.buildReturn(firstParameter)
			constructor.nullPointer
		} else {
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			parameters.add(Context.THIS_PARAMETER_INDEX, context.getThisParameter(constructor))
			constructor.buildFunctionCall(initializerModel.unit.llvmType, initializerModel.unit.llvmValue, parameters)
			context.continueRaise(constructor, model)
			constructor.nullPointer
		}
	}

	private fun buildLlvmCommonPreInitializerCall(constructor: LlvmConstructor,
												  typeDeclaration: components.code_generation.llvm.models.declarations.TypeDeclaration,
												  exceptionAddress: LlvmValue, newObject: LlvmValue) {
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
		if(typeDeclaration.model.isBound) {
			val parent = (function as? components.code_generation.llvm.models.operations.MemberAccess)?.target?.getLlvmValue(constructor)
				?: context.getThisParameter(constructor)
			parameters.add(Context.PARENT_PARAMETER_OFFSET, parent)
		}
		//TODO how are complex types passed? e.g. <Int, Byte>Map or Int? or Int | Cat
		// What are they used for?
		// -> type check (could be limited to ObjectType)
		// -> instantiation (only works with ObjectTypes, may include generic types)
		for(typeParameter in model.globalTypeParameters) {
			val objectType = typeParameter.effectiveType as? ObjectType
				?: throw CompilerError(typeParameter.source, "Only object types are allowed as type parameters.")
			parameters.add(objectType.getStaticLlvmValue(constructor))
		}
		constructor.buildFunctionCall(typeDeclaration.commonClassPreInitializer, parameters)
		context.continueRaise(constructor, model)
	}
}
