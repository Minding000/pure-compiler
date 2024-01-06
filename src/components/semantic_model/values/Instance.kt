package components.semantic_model.values

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.ValueConverter
import components.semantic_model.context.Context
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.SelfType
import components.semantic_model.types.StaticType
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
		this.typeDeclaration = scope.getSurroundingTypeDeclaration()
			?: throw CompilerError(source, "Instance outside of type definition.")
		val type = SelfType(typeDeclaration)
		addSemanticModels(type)
		this.type = type
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
		signature += valueParameters.joinToString { parameter -> parameter.type.toString() }
		signature += ")"
		return signature
	}

	fun getLlvmValue(constructor: LlvmConstructor): LlvmValue {
		if(isNative)
			return context.getNativeInstanceValue(constructor, "${typeDeclaration.name}.$memberIdentifier")
		val initializer = initializer ?: throw CompilerError(source, "Missing initializer in instance declaration.")
		val exceptionAddress = context.getExceptionParameter(constructor)
		val typeDeclaration = initializer.parentTypeDeclaration
		val instance = constructor.buildHeapAllocation(typeDeclaration.llvmType, "${typeDeclaration.name}_${name}_Instance")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(typeDeclaration.llvmType, instance,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
		constructor.buildStore(typeDeclaration.llvmClassDefinition, classDefinitionProperty)
		buildLlvmCommonPreInitializerCall(constructor, typeDeclaration, exceptionAddress, instance)
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, instance)
		for((index, valueParameter) in valueParameters.withIndex())
			parameters.add(ValueConverter.convertIfRequired(this, constructor, valueParameter.getLlvmValue(constructor),
				valueParameter.type, initializer.getParameterTypeAt(index), conversions?.get(valueParameter)))
		if(initializer.isVariadic) {
			val fixedParameterCount = initializer.fixedParameters.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters)
		context.continueRaise()
		return instance
	}

	private fun buildLlvmCommonPreInitializerCall(constructor: LlvmConstructor, typeDeclaration: TypeDeclaration,
												  exceptionAddress: LlvmValue, newObject: LlvmValue) {
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
		constructor.buildFunctionCall(typeDeclaration.llvmCommonPreInitializerType, typeDeclaration.llvmCommonPreInitializer, parameters)
		context.continueRaise()
	}
}
