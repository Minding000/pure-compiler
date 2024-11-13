package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.general.ErrorHandlingContext
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmType
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.PrimitiveImplementation
import components.semantic_model.declarations.ComputedPropertyDeclaration
import errors.internal.CompilerError
import java.util.*

class ComputedPropertyDeclaration(override val model: ComputedPropertyDeclaration,
								  val getterErrorHandlingContext: ErrorHandlingContext?,
								  val setterErrorHandlingContext: ErrorHandlingContext?):
	ValueDeclaration(model, null, listOfNotNull(getterErrorHandlingContext, setterErrorHandlingContext)) {
	var llvmGetterType: LlvmType? = null
	var llvmSetterType: LlvmType? = null
	var llvmGetterValue: LlvmValue? = null
	var llvmSetterValue: LlvmValue? = null

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		if(model.isGettable || getterErrorHandlingContext != null) {
			if(!model.isAbstract && model.parentTypeDeclaration.isLlvmPrimitive())
				context.primitiveCompilationTarget = model.parentTypeDeclaration
			val llvmType = model.root.effectiveType?.getLlvmType(constructor)
			val functionType = constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType), llvmType)
			llvmGetterType = functionType
			if(!model.isAbstract) {
				llvmGetterValue = constructor.buildFunction(getGetterSignature(), functionType)
				if(model.parentTypeDeclaration.isLlvmPrimitive()) {
					context.primitiveCompilationTarget = null
					declarePrimitiveGetterImplementation(constructor)
				}
			}
		}
		if(model.isSettable || setterErrorHandlingContext != null) {
			val llvmType = model.root.effectiveType?.getLlvmType(constructor)
			val functionType = constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType, llvmType))
			llvmSetterType = functionType
			if(!model.isAbstract)
				llvmSetterValue = constructor.buildFunction(model.setterIdentifier, functionType)
		}
	}

	private fun declarePrimitiveGetterImplementation(constructor: LlvmConstructor) {
		val llvmType = model.effectiveType?.getLlvmType(constructor)
		val functionType =
			constructor.buildFunctionType(listOf(constructor.pointerType, model.parentTypeDeclaration.getLlvmReferenceType(constructor)),
				llvmType)
		val signature = getGetterSignature()
		val functionValue = constructor.buildFunction("${signature}_PrimitiveImplementation", functionType)
		context.nativeRegistry.registerPrimitiveImplementation(signature, PrimitiveImplementation(functionValue, functionType))
	}

	override fun compile(constructor: LlvmConstructor) {
		if(model.isAbstract)
			return
		val previousBlock = constructor.getCurrentBlock()
		if(model.isNative) {
			if(model.isGettable) {
				val llvmGetterValue = llvmGetterValue ?: throw CompilerError(this, "Missing getter value")
				context.nativeRegistry.compileNativeImplementation(constructor, model.source, "computed property getter",
					model.getterIdentifier, llvmGetterValue)
			}
			if(model.isSettable) {
				val llvmSetterValue = llvmSetterValue ?: throw CompilerError(this, "Missing setter value")
				context.nativeRegistry.compileNativeImplementation(constructor, model.source, "computed property setter",
					model.setterIdentifier, llvmSetterValue)
			}
			constructor.select(previousBlock)
			return
		}
		if(model.parentTypeDeclaration.isLlvmPrimitive()) {
			compilePrimitiveGetterImplementation(constructor)
			compileObjectGetterImplementationBasedOnPrimitiveImplementation(constructor)
			constructor.select(previousBlock)
			return
		}
		val llvmGetterValue = llvmGetterValue
		if(llvmGetterValue != null && getterErrorHandlingContext != null) {
			constructor.createAndSelectEntrypointBlock(llvmGetterValue)
			getterErrorHandlingContext.compile(constructor)
		}
		val llvmSetterValue = llvmSetterValue
		if(llvmSetterValue != null && setterErrorHandlingContext != null) {
			constructor.createAndSelectEntrypointBlock(llvmSetterValue)
			setterErrorHandlingContext.compile(constructor)
			if(!setterErrorHandlingContext.model.isInterruptingExecutionBasedOnStructure)
				constructor.buildReturn()
		}
		constructor.select(previousBlock)
	}

	private fun compilePrimitiveGetterImplementation(constructor: LlvmConstructor) {
		val primitiveImplementation = context.nativeRegistry.resolvePrimitiveImplementation(getGetterSignature())
		constructor.createAndSelectEntrypointBlock(primitiveImplementation.llvmValue)
		getterErrorHandlingContext?.compile(constructor)
	}

	private fun compileObjectGetterImplementationBasedOnPrimitiveImplementation(constructor: LlvmConstructor) {
		val llvmGetterValue = llvmGetterValue ?: throw CompilerError(this, "Missing getter value")
		constructor.createAndSelectEntrypointBlock(llvmGetterValue)
		val unwrappedParameters = LinkedList<LlvmValue?>()
		unwrappedParameters.add(Context.EXCEPTION_PARAMETER_INDEX, context.getExceptionParameter(constructor))
		val thisParameter = context.getThisParameter(constructor)
		unwrappedParameters.add(Context.THIS_PARAMETER_INDEX,
			ValueConverter.unwrapPrimitive(model, constructor, thisParameter, model.parentTypeDeclaration))
		val signature = getGetterSignature()
		val primitiveImplementation = context.nativeRegistry.resolvePrimitiveImplementation(signature)
		var result = constructor.buildFunctionCall(primitiveImplementation.llvmType, primitiveImplementation.llvmValue, unwrappedParameters,
			signature)
		result = ValueConverter.wrapPrimitive(model, constructor, result, model.effectiveType)
		constructor.buildReturn(result)
	}

	fun getGetterSignature(): String {
		return "${model.parentTypeDeclaration.name}.get ${model.memberIdentifier}"
	}
}
