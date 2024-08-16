package components.semantic_model.declarations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import errors.internal.CompilerError
import java.util.*
import components.syntax_parser.syntax_tree.definitions.TypeAlias as TypeAliasSyntaxTree

// Consideration:
// Should TypeAliases mask the instances of the aliased type?
// Should the behaviour be toggleable using a keyword flag e.g. "masking alias ExitCode = Int"
class TypeAlias(override val source: TypeAliasSyntaxTree, scope: TypeScope, name: String, val referenceType: Type,
				val instances: List<Instance>): TypeDeclaration(source, name, scope, null, null, instances) {
	override val isDefinition = false
	private var hasDeterminedEffectiveType = false
	private var effectiveType = referenceType

	init {
		scope.typeDeclaration = this
		addSemanticModels(referenceType)
	}

	override fun getValueDeclaration(): ValueDeclaration {
		val targetScope = parentTypeDeclaration?.scope ?: scope.enclosingScope
		val staticType = StaticType(this)
		staticValueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, staticType, null, true)
		else
			GlobalValueDeclaration(source, targetScope, name, staticType)
		return staticValueDeclaration
	}

	fun getEffectiveType(): Type {
		if(!context.declarationStack.push(this))
			return effectiveType
		if(!hasDeterminedEffectiveType) {
			hasDeterminedEffectiveType = true
			referenceType.determineTypes()
			if(referenceType is ObjectType) {
				val referenceTypeDeclaration = referenceType.getTypeDeclaration()
				if(referenceTypeDeclaration is TypeAlias)
					effectiveType = referenceTypeDeclaration.effectiveType
			}
		}
		context.declarationStack.pop(this)
		return effectiveType
	}

	override fun declare() {
		super.declare()
		scope.enclosingScope.addTypeDeclaration(this)
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return referenceType.getConversionsFrom(sourceType)
	}

	override fun define(constructor: LlvmConstructor) {
		super.declare(constructor)
		val llvmType = effectiveType.getLlvmType(constructor)
		for(instance in instances) {
			instance.llvmLocation = constructor.declareGlobal("${name}_${instance.name}_TypeAliasInstance", llvmType)
			constructor.defineGlobal(instance.llvmLocation, context.getNullValue(constructor, effectiveType))
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
		val initializer = instance.initializer ?: throw CompilerError(source, "Missing initializer in type alias instance declaration.")
		val typeDeclaration = initializer.parentTypeDeclaration
		val parameters = LinkedList<LlvmValue?>()
		for((index, valueParameter) in instance.valueParameters.withIndex())
			parameters.add(ValueConverter.convertIfRequired(this, constructor, valueParameter.getLlvmValue(constructor),
				valueParameter.providedType, initializer.getParameterTypeAt(index), instance.conversions?.get(valueParameter)))
		if(initializer.isVariadic) {
			val fixedParameterCount = initializer.fixedParameters.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		if(initializer.parentTypeDeclaration.isLlvmPrimitive()) {
			val signature = initializer.toString()
			if(initializer.isNative)
				return context.nativeRegistry.inlineNativePrimitiveInitializer(constructor, "${signature}: Self", parameters)
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			return constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters, signature)
		}
		val instanceValue = constructor.buildHeapAllocation(typeDeclaration.llvmType, "${typeDeclaration.name}_${name}_Instance")
		buildLlvmCommonPreInitializerCall(constructor, typeDeclaration, exceptionAddress, instanceValue)
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, instanceValue)
		constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters)
		context.continueRaise(constructor, this)
		return instanceValue
	}

	private fun buildLlvmCommonPreInitializerCall(constructor: LlvmConstructor, typeDeclaration: TypeDeclaration,
												  exceptionAddress: LlvmValue, newObject: LlvmValue) {
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
		constructor.buildFunctionCall(typeDeclaration.llvmCommonPreInitializerType, typeDeclaration.llvmCommonPreInitializer, parameters)
		context.continueRaise(constructor, this)
	}
}
