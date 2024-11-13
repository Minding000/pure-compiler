package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.general.ErrorHandlingContext
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.PrimitiveImplementation
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.types.SelfType
import java.util.*

class FunctionDefinition(override val model: FunctionImplementation, val parameters: List<Parameter>, val body: ErrorHandlingContext?):
	Unit(model, listOfNotNull(*parameters.toTypedArray(), body)) {
	lateinit var llvmValue: LlvmValue

	override fun declare(constructor: LlvmConstructor) {
		if(model.isAbstract)
			return
		super.declare(constructor)
		if(model.parentTypeDeclaration?.isLlvmPrimitive() == true)
			context.primitiveCompilationTarget = model.parentTypeDeclaration
		//TODO add local type parameters
		for(index in parameters.indices)
			parameters[index].index = index + Context.VALUE_PARAMETER_OFFSET
		llvmValue = constructor.buildFunction(model.memberIdentifier, model.signature.getLlvmType(constructor))
		if(model.parentTypeDeclaration?.isLlvmPrimitive() == true) {
			context.primitiveCompilationTarget = null
			// Assumption: Native declarations are inlined, so there is no need for an implementation
			if(!model.isNative)
				declarePrimitiveImplementation(constructor)
		}
	}

	private fun declarePrimitiveImplementation(constructor: LlvmConstructor) {
		val functionType = model.signature.buildLlvmType(constructor)
		val signature = model.toString()
		val functionValue = constructor.buildFunction("${signature}_PrimitiveImplementation", functionType)
		context.nativeRegistry.registerPrimitiveImplementation(signature, PrimitiveImplementation(functionValue, functionType))
	}

	override fun compile(constructor: LlvmConstructor) {
		if(model.isAbstract)
			return
		val previousBlock = constructor.getCurrentBlock()
		if(model.isNative) {
			context.nativeRegistry.compileNativeImplementation(constructor, model, llvmValue)
			constructor.select(previousBlock)
			return
		}
		if(model.parentTypeDeclaration?.isLlvmPrimitive() == true) {
			compilePrimitiveImplementation(constructor)
			compileObjectImplementationBasedOnPrimitiveImplementation(constructor)
			constructor.select(previousBlock)
			return
		}
		constructor.createAndSelectEntrypointBlock(llvmValue)
		super.compile(constructor)
		if(body?.model?.isInterruptingExecutionBasedOnStructure != true)
			constructor.buildReturn()
		constructor.select(previousBlock)
	}

	private fun compilePrimitiveImplementation(constructor: LlvmConstructor) {
		val primitiveImplementation = context.nativeRegistry.resolvePrimitiveImplementation(model.toString())
		constructor.createAndSelectEntrypointBlock(primitiveImplementation.llvmValue)
		super.compile(constructor)
		if(body?.model?.isInterruptingExecutionBasedOnStructure != true)
			constructor.buildReturn()
	}

	private fun compileObjectImplementationBasedOnPrimitiveImplementation(constructor: LlvmConstructor) {
		constructor.createAndSelectEntrypointBlock(llvmValue)
		val unwrappedParameters = LinkedList<LlvmValue?>()
		unwrappedParameters.add(Context.EXCEPTION_PARAMETER_INDEX, context.getExceptionParameter(constructor))
		val thisParameter = context.getThisParameter(constructor)
		unwrappedParameters.add(Context.THIS_PARAMETER_INDEX,
			ValueConverter.unwrapPrimitive(model, constructor, thisParameter, model.parentTypeDeclaration))
		for(parameter in parameters) {
			var value = constructor.getParameter(parameter.index)
			val type = parameter.model.providedType
			if(type is SelfType && type.typeDeclaration == model.parentTypeDeclaration)
				value = ValueConverter.unwrapPrimitive(model, constructor, value, type)
			unwrappedParameters.add(value)
		}
		val signatureString = model.toString()
		val primitiveImplementation = context.nativeRegistry.resolvePrimitiveImplementation(signatureString)
		val doesReturn = !SpecialType.NOTHING.matches(model.signature.returnType)
		val resultName = if(doesReturn) signatureString else ""
		var result = constructor.buildFunctionCall(primitiveImplementation.llvmType, primitiveImplementation.llvmValue, unwrappedParameters,
			resultName)
		if(doesReturn) {
			if(model.signature.returnType is SelfType && model.signature.returnType.typeDeclaration == model.parentTypeDeclaration)
				result = ValueConverter.wrapPrimitive(model, constructor, result, model.signature.returnType)
			constructor.buildReturn(result)
		}
		if(body?.model?.isInterruptingExecutionBasedOnStructure != true)
			constructor.buildReturn()
	}

	//TODO add debug info
	@Suppress("unused")
	private fun addDebugInfo(constructor: LlvmConstructor) {
		val file = constructor.debug.createFile("test.pure", ".")
		val parent = file
		val parameterMetadata = parameters.map { parameter -> parameter.model.providedType?.getLlvmMetadata(constructor) }
		val typeMetadata = constructor.debug.createFunctionType(file, parameterMetadata)
		val metadata = constructor.debug.createFunction(parent, model.toString(), file, typeMetadata)
		constructor.debug.attach(metadata, llvmValue)
	}
}
