package components.semantic_model.declarations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.SelfType
import components.semantic_model.types.StaticType
import components.semantic_model.values.Value
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import java.util.*
import components.syntax_parser.syntax_tree.definitions.Instance as InstanceSyntaxTree

//TODO disallow instances in bound classes
class Instance(override val source: InstanceSyntaxTree, scope: MutableScope, name: String, val valueParameters: List<Value>,
			   isAbstract: Boolean, isOverriding: Boolean, val isNative: Boolean):
	InterfaceMember(source, scope, name, null, null, true, isAbstract, true, false, isOverriding) {
	lateinit var typeDeclaration: TypeDeclaration
	var initializer: InitializerDefinition? = null
	var conversions: Map<Value, InitializerDefinition>? = null

	init {
		addSemanticModels(valueParameters)
	}

	override fun determineType() {
		typeDeclaration = scope.getSurroundingTypeDeclaration()
			?: throw CompilerError(source, "Instance outside of type definition.")
		run {
			val typeDeclaration = typeDeclaration
			if(typeDeclaration is TypeAlias) {
				//TODO this should be an issue instead
				val effectiveObjectType = typeDeclaration.getEffectiveType() as? ObjectType
					?: throw CompilerError(source, "Instances are not allowed in inconcrete type aliases.")
				this.typeDeclaration = effectiveObjectType.getTypeDeclaration() ?: return
			}
		}
		val type = SelfType(typeDeclaration)
		addSemanticModels(type)
		providedType = type
		val staticType = StaticType(typeDeclaration)
		addSemanticModels(staticType)
		super.determineType()
		if(isAbstract || isNative)
			return
		try {
			val match = staticType.getInitializer(valueParameters)
			if(match == null) {
				context.addIssue(NotFound(source, "Initializer", getSignature()))
				return
			}
			initializer = match.initializer
			conversions = match.conversions
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "initializer", getSignature())
		}
	}

	private fun getSignature(): String {
		var signature = typeDeclaration.name
		signature += "("
		signature += valueParameters.joinToString { parameter -> parameter.providedType.toString() }
		signature += ")"
		return signature
	}

	fun getLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val initializer = initializer ?: throw CompilerError(source, "Missing initializer in instance declaration.")
		val exceptionAddress = context.getExceptionParameter(constructor)
		val parameters = LinkedList<LlvmValue?>()
		for((index, valueParameter) in valueParameters.withIndex())
			parameters.add(ValueConverter.convertIfRequired(this, constructor, valueParameter.getLlvmValue(constructor),
				valueParameter.providedType, initializer.getParameterTypeAt(index), conversions?.get(valueParameter)))
		if(initializer.isVariadic) {
			val fixedParameterCount = initializer.fixedParameters.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		if(initializer.parentTypeDeclaration.isLlvmPrimitive()) {
			val signature = initializer.toString()
			if(initializer.isNative)
				return context.nativeRegistry.inlineNativePrimitiveInitializer(constructor, "$signature: Self", parameters)
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			return constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters, signature)
		}
		val typeDeclaration = initializer.parentTypeDeclaration
		val instance = constructor.buildHeapAllocation(typeDeclaration.llvmType, "${typeDeclaration.name}_${name}_Instance")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(typeDeclaration.llvmType, instance,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
		constructor.buildStore(typeDeclaration.llvmClassDefinition, classDefinitionProperty)
		buildLlvmCommonPreInitializerCall(constructor, typeDeclaration, exceptionAddress, instance)
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, instance)
		constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters)
		context.continueRaise(constructor, this)
		return instance
	}

	private fun buildLlvmCommonPreInitializerCall(constructor: LlvmConstructor, typeDeclaration: TypeDeclaration,
												  exceptionAddress: LlvmValue, newObject: LlvmValue) {
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
		constructor.buildFunctionCall(typeDeclaration.llvmCommonPreInitializerType, typeDeclaration.llvmCommonPreInitializer, parameters)
		context.continueRaise(constructor, this)
	}

	override fun requiresFileRunner(): Boolean {
		//TODO What about isNative?
		return !isAbstract
	}
}
