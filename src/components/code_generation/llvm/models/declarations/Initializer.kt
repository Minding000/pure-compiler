package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.models.general.ErrorHandlingContext
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmType
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.types.StaticType
import errors.internal.CompilerError
import java.util.*

class Initializer(override val model: InitializerDefinition, val parameters: List<Parameter>, val body: ErrorHandlingContext?):
	Unit(model, listOfNotNull(*parameters.toTypedArray(), body)) {
	lateinit var llvmValue: LlvmValue
	lateinit var llvmType: LlvmType

	override fun declare(constructor: LlvmConstructor) {
		if(model.isAbstract)
			return
		super.declare(constructor)
		val parameterTypes = LinkedList<LlvmType?>()
		parameterTypes.add(Context.EXCEPTION_PARAMETER_INDEX, constructor.pointerType)
		var parameterIndex = Context.THIS_PARAMETER_INDEX
		if(!model.parentTypeDeclaration.isLlvmPrimitive()) {
			parameterTypes.add(Context.THIS_PARAMETER_INDEX, constructor.pointerType)
			parameterIndex++
		}
		//TODO add local type parameters
		for(valueParameter in parameters) {
			parameterTypes.add(valueParameter.model.effectiveType?.getLlvmType(constructor))
			valueParameter.index = parameterIndex
			parameterIndex++
		}
		llvmType = if(!model.isNative && model.parentTypeDeclaration.isLlvmPrimitive())
			constructor.buildFunctionType(parameterTypes, getPrimitiveLlvmType(constructor, model.parentTypeDeclaration), model.isVariadic)
		else
			constructor.buildFunctionType(parameterTypes, constructor.voidType, model.isVariadic)
		llvmValue = constructor.buildFunction("${model.parentTypeDeclaration.getFullName()}_Initializer", llvmType)
	}

	private fun getPrimitiveLlvmType(constructor: LlvmConstructor, typeDeclaration: TypeDeclaration): LlvmType {
		if(SpecialType.BOOLEAN.matches(typeDeclaration))
			return constructor.booleanType
		if(SpecialType.BYTE.matches(typeDeclaration))
			return constructor.byteType
		if(SpecialType.INTEGER.matches(typeDeclaration))
			return constructor.i32Type
		if(SpecialType.FLOAT.matches(typeDeclaration))
			return constructor.floatType
		throw CompilerError(model, "Encountered unknown primitive type declaration '${typeDeclaration.name}'.")
	}

	override fun compile(constructor: LlvmConstructor) {
		if(model.isAbstract)
			return
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectEntrypointBlock(llvmValue)
		val thisValue = context.getThisParameter(constructor, llvmValue)
		//TODO add local type parameters
		for(valueParameter in parameters) {
			if(valueParameter.model.isPropertySetter) {
				val propertyAddress = context.resolveMember(constructor, thisValue, valueParameter.model.name)
				constructor.buildStore(constructor.getParameter(llvmValue, valueParameter.index), propertyAddress)
			}
		}
		if(model.parentTypeDeclaration.isLlvmPrimitive()) {
			if(model.isNative)
				constructor.buildReturn()
			else
				super.compile(constructor)
		} else {
			if(model.isNative)
				context.nativeRegistry.compileNativeImplementation(constructor, model, llvmValue)
			else if(body == null)
				callTrivialSuperInitializers(constructor, thisValue)
			else
				super.compile(constructor)
			if(body?.model?.isInterruptingExecutionBasedOnStructure != true)
				constructor.buildReturn()
		}
		constructor.select(previousBlock)
	}

	private fun callTrivialSuperInitializers(constructor: LlvmConstructor, thisValue: LlvmValue) {
		val exceptionAddress = context.getExceptionParameter(constructor, llvmValue)
		for(superType in model.parentTypeDeclaration.getDirectSuperTypes()) {
			if(SpecialType.IDENTIFIABLE.matches(superType) || SpecialType.ANY.matches(superType))
				continue
			val superTypeDeclaration = superType.getTypeDeclaration()
			val trivialInitializer =
				(superTypeDeclaration?.staticValueDeclaration?.providedType as? StaticType)?.getInitializer()?.initializer?.unit
					?: throw CompilerError(model, "Default initializer in class '${model.parentTypeDeclaration.name}'" +
						" with super class '${superTypeDeclaration?.name}' without trivial initializer.")
			val parameters = LinkedList<LlvmValue?>()
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			parameters.add(Context.THIS_PARAMETER_INDEX, thisValue)
			constructor.buildFunctionCall(trivialInitializer.llvmType, trivialInitializer.llvmValue, parameters)
			context.continueRaise(constructor, model)
		}
	}
}
