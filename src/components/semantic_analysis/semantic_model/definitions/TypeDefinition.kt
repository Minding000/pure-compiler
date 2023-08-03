package components.semantic_analysis.semantic_model.definitions

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.Context
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.AndUnionType
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.definition.CircularInheritance
import logger.issues.definition.ExplicitParentOnScopedTypeDefinition
import logger.issues.modifiers.NoParentToBindTo
import util.linkedListOf
import java.util.*

abstract class TypeDefinition(override val source: SyntaxTreeNode, val name: String, override val scope: TypeScope,
							  val explicitParentType: ObjectType? = null, val superType: Type? = null, val members: List<SemanticModel> = listOf(),
							  val isBound: Boolean = false, val isSpecificCopy: Boolean = false): SemanticModel(source, scope) {
	protected open val isDefinition = true
	override var parent: SemanticModel?
		get() = super.parent
		set(value) {
			super.parent = value
			parentTypeDefinition = value as? TypeDefinition
		}
	var parentTypeDefinition: TypeDefinition? = null
	private var hasCircularInheritance = false
	var hasDeterminedTypes = isSpecificCopy
	// Only used in base definition
	private val specificDefinitions = HashMap<Map<TypeDefinition, Type>, TypeDefinition>()
	private val pendingTypeSubstitutions = HashMap<Map<TypeDefinition, Type>, LinkedList<(TypeDefinition) -> Unit>>()
	// Only used in specific definition
	var baseDefinition: TypeDefinition? = null
	lateinit var staticValueDeclaration: ValueDeclaration
	lateinit var properties: List<ValueDeclaration>
	lateinit var llvmType: LlvmType
	lateinit var llvmClassDefinitionAddress: LlvmValue
	lateinit var llvmClassInitializer: LlvmValue
	lateinit var llvmClassInitializerType: LlvmType
	lateinit var llvmStaticType: LlvmType

	init {
		addSemanticModels(explicitParentType, superType)
		addSemanticModels(members)
		for(member in members) {
			if(member is InterfaceMember)
				member.parentDefinition = this
		}
	}

	protected abstract fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): TypeDefinition

	fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>, onCompletion: (TypeDefinition) -> Unit) {
		var definition = specificDefinitions[typeSubstitutions]
		if(definition != null) {
			onCompletion(definition)
			return
		}
		var pendingTypeSubstitution = pendingTypeSubstitutions[typeSubstitutions]
		if(pendingTypeSubstitution != null) {
			pendingTypeSubstitution.add(onCompletion)
			return
		}
		pendingTypeSubstitution = linkedListOf(onCompletion)
		pendingTypeSubstitutions[typeSubstitutions] = pendingTypeSubstitution
		definition = withTypeSubstitutions(typeSubstitutions)
		specificDefinitions[typeSubstitutions] = definition
		for(onTypeSubstitution in pendingTypeSubstitution)
			onTypeSubstitution(definition)
		pendingTypeSubstitutions.remove(typeSubstitutions)
	}

	fun withTypeParameters(typeParameters: List<Type>, onCompletion: (TypeDefinition) -> Unit) =
		withTypeParameters(typeParameters, HashMap<TypeDefinition, Type>(), onCompletion)

	fun withTypeParameters(typeParameters: List<Type>, typeSubstitutions: Map<TypeDefinition, Type>,
						   onCompletion: (TypeDefinition) -> Unit) {
		baseDefinition?.let { baseDefinition ->
			return baseDefinition.withTypeParameters(typeParameters, typeSubstitutions, onCompletion)
		}
		val resolvedTypeSubstitutions = typeSubstitutions.toMutableMap()
		val placeholders = scope.getGenericTypeDefinitions()
		for(parameterIndex in placeholders.indices) {
			val placeholder = placeholders[parameterIndex]
			val typeParameter = typeParameters.getOrNull(parameterIndex) ?: break
			resolvedTypeSubstitutions[placeholder] = typeParameter
		}
		withTypeSubstitutions(resolvedTypeSubstitutions) { specificTypeDefinition ->
			specificTypeDefinition.baseDefinition = this
			onCompletion(specificTypeDefinition)
		}
	}

	fun getLinkedSuperType(): Type? {
		superType?.determineTypes()
		return superType
	}

	open fun getValueDeclaration(): ValueDeclaration? = null

	override fun determineTypes() {
		if(hasDeterminedTypes)
			return
		hasDeterminedTypes = true
		explicitParentType?.determineTypes()
		if(explicitParentType != null) {
			if(parentTypeDefinition == null) {
				parentTypeDefinition = explicitParentType.definition
			} else {
				context.addIssue(ExplicitParentOnScopedTypeDefinition(explicitParentType.source))
			}
		}
		for(semanticModel in semanticModels)
			if(semanticModel !== explicitParentType)
				semanticModel.determineTypes()
		if(isDefinition && scope.initializers.isEmpty())
			addDefaultInitializer()
		scope.ensureUniqueInitializerSignatures()
		scope.inheritSignatures()
		if(superType != null && inheritsFrom(this)) {
			hasCircularInheritance = true
			context.addIssue(CircularInheritance(superType.source))
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		for(member in scope.memberDeclarations) {
			if(member is FunctionImplementation)
				member.analyseDataFlow(tracker)
		}
		if(!hasCircularInheritance) {
			for(initializer in scope.initializers)
				initializer.analyseDataFlow(tracker)
		}
	}

	override fun validate() {
		super.validate()
		if(isBound && parentTypeDefinition == null)
			context.addIssue(NoParentToBindTo(source))
		if(isDefinition && (this as? Class)?.isAbstract != true && !hasCircularInheritance)
			scope.ensureAbstractSuperMembersImplemented()
	}

	private fun addDefaultInitializer() {
		val defaultInitializer = InitializerDefinition(source, BlockScope(scope))
		defaultInitializer.determineTypes()
		addSemanticModels(defaultInitializer)
	}

	private fun inheritsFrom(definition: TypeDefinition): Boolean {
		for(superType in getDirectSuperTypes()) {
			superType.determineTypes()
			if(superType.definition == definition)
				return true
			if(superType.definition?.inheritsFrom(definition) == true)
				return true
		}
		return false
	}

	fun getAllSuperTypes(): List<ObjectType> {
		val superTypes = LinkedList<ObjectType>()
		for(superType in getDirectSuperTypes()) {
			superTypes.add(superType)
			superTypes.addAll(superType.definition?.getAllSuperTypes() ?: continue)
		}
		return superTypes
	}

	fun getDirectSuperTypes(): List<ObjectType> {
		return when(val superType = superType) {
			is ObjectType -> listOf(superType)
			is AndUnionType -> superType.types.filterIsInstance<ObjectType>()
			else -> listOf()
		}
	}

	open fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		determineTypes()
		return scope.getConversionsFrom(sourceType)
	}

	fun acceptsSubstituteType(substituteType: Type): Boolean {
		if(SpecialType.ANY.matches(superType))
			return true
		return superType?.accepts(substituteType) ?: false
	}

	override fun equals(other: Any?): Boolean {
		if(other !is TypeDefinition)
			return false
		return source == other.source
	}

	override fun hashCode(): Int {
		return source.hashCode()
	}

	override fun declare(constructor: LlvmConstructor) {
		if(isDefinition) {
			llvmClassInitializerType = constructor.buildFunctionType()
			llvmClassInitializer = constructor.buildFunction("${name}_ClassInitializer", llvmClassInitializerType)
			llvmStaticType = constructor.declareStruct("${name}_StaticStruct")
			llvmType = constructor.declareStruct("${name}_ClassStruct")
		}
		super.declare(constructor)
	}

	override fun define(constructor: LlvmConstructor) {
		if(isDefinition) {
			val constants = getConstants().values.toList()
			this.properties = getProperties().values.toList()
			val functions = getFunctions().values.toList()
			defineLlvmStruct(constructor, constants, this.properties)
			defineLlvmClassInitializer(constructor, constants, this.properties, functions)
		}
		super.define(constructor)
	}

	private fun getConstants(): Map<String, ValueDeclaration> {
		val constants = HashMap<String, ValueDeclaration>()
		for(member in scope.memberDeclarations)
			if((member as? InterfaceMember)?.isStatic == true)
				constants[member.name] = member
		for(superType in getDirectSuperTypes()) {
			for(constant in superType.definition?.getConstants()?.values ?: emptyList())
				constants.putIfAbsent(constant.name, constant)
		}
		return constants
	}

	private fun getProperties(): Map<String, ValueDeclaration> {
		val properties = HashMap<String, ValueDeclaration>()
		for(member in scope.memberDeclarations)
			if(member is ValueDeclaration && (member as? InterfaceMember)?.isStatic != true)
				properties[member.name] = member
		for(superType in getDirectSuperTypes()) {
			for(property in superType.definition?.getProperties()?.values ?: emptyList())
				properties.putIfAbsent(property.name, property)
		}
		return properties
	}

	private fun getFunctions(): Map<String, FunctionImplementation> {
		val functions = HashMap<String, FunctionImplementation>()
		for(member in scope.memberDeclarations)
			if(member is FunctionImplementation)
				functions[member.memberIdentifier] = member
		for(superType in getDirectSuperTypes()) {
			for(function in superType.definition?.getFunctions()?.values ?: emptyList())
				functions.putIfAbsent(function.memberIdentifier, function)
		}
		return functions
	}

	private fun defineLlvmStruct(constructor: LlvmConstructor, constants: List<ValueDeclaration>, properties: List<ValueDeclaration>) {
		val llvmConstants = LinkedList<LlvmType?>()
		llvmConstants.add(constructor.createPointerType(context.classDefinitionStruct))
		for(memberDeclaration in constants)
			llvmConstants.add(memberDeclaration.type?.getLlvmType(constructor))
		constructor.defineStruct(llvmStaticType, llvmConstants)
		val llvmProperties = LinkedList<LlvmType?>()
		llvmProperties.add(constructor.createPointerType(context.classDefinitionStruct))
		for(memberDeclaration in properties)
			llvmProperties.add(memberDeclaration.type?.getLlvmType(constructor))
		constructor.defineStruct(llvmType, llvmProperties)
	}

	private fun defineLlvmClassInitializer(constructor: LlvmConstructor, constants: List<ValueDeclaration>, properties: List<ValueDeclaration>, functions: List<FunctionImplementation>) {
		println("'$name' class initializer:")
		constructor.createAndSelectBlock(llvmClassInitializer, "entrypoint")
		for(typeDefinition in scope.typeDefinitions.values)
			constructor.buildFunctionCall(typeDefinition.llvmClassInitializerType, typeDefinition.llvmClassInitializer)
		val constantCount = constructor.buildInt32(constants.size)
		val constantIdArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, constantCount, "constantIdArray")
		val constantOffsetArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberOffsetType, constantCount, "constantOffsetArray")
		val propertyCount = constructor.buildInt32(properties.size)
		val propertyIdArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, propertyCount, "propertyIdArray")
		val propertyOffsetArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberOffsetType, propertyCount, "propertyOffsetArray")
		val functionCount = constructor.buildInt32(functions.size)
		val functionIdArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, functionCount, "functionIdArray")
		val functionAddressArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberAddressType, functionCount, "functionAddressArray")
		for((memberIndex, memberDeclaration) in constants.withIndex()) {
			val memberId = context.memberIdentities.register(memberDeclaration.name)
			val structMemberIndex = memberIndex + 1
			val memberOffset = constructor.getMemberOffsetInBytes(llvmStaticType, structMemberIndex)
			println("Mapping constant '${memberDeclaration.name}' to ID '$memberId' with offset '$memberOffset'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, constantIdArrayAddress, memberIndexValue, "constantIdLocation")
			val offsetLocation = constructor.buildGetArrayElementPointer(context.llvmMemberOffsetType, constantOffsetArrayAddress, memberIndexValue, "constantOffsetLocation")
			val memberIdValue = constructor.buildInt32(memberId)
			val memberOffsetValue = constructor.buildInt32(memberOffset)
			constructor.buildStore(memberIdValue, idLocation)
			constructor.buildStore(memberOffsetValue, offsetLocation)
		}
		for((memberIndex, property) in properties.withIndex()) {
			val memberId = context.memberIdentities.register(property.name)
			val structMemberIndex = memberIndex + 1
			val memberOffset = constructor.getMemberOffsetInBytes(llvmType, structMemberIndex)
			println("Mapping property '${property.name}' to ID '$memberId' with offset '$memberOffset'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, propertyIdArrayAddress, memberIndexValue, "propertyIdLocation")
			val offsetLocation = constructor.buildGetArrayElementPointer(context.llvmMemberOffsetType, propertyOffsetArrayAddress, memberIndexValue, "propertyOffsetLocation")
			val memberIdValue = constructor.buildInt32(memberId)
			val memberOffsetValue = constructor.buildInt32(memberOffset)
			constructor.buildStore(memberIdValue, idLocation)
			constructor.buildStore(memberOffsetValue, offsetLocation)
		}
		for((memberIndex, function) in functions.withIndex()) {
			val memberId = context.memberIdentities.register(function.memberIdentifier)
			println("Mapping function '${function.memberIdentifier}' to ID '$memberId'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, functionIdArrayAddress, memberIndexValue, "functionIdLocation")
			val addressLocation = constructor.buildGetArrayElementPointer(context.llvmMemberAddressType, functionAddressArrayAddress, memberIndexValue, "functionAddressLocation")
			val memberIdValue = constructor.buildInt32(memberId)
			constructor.buildStore(memberIdValue, idLocation)
			constructor.buildStore(function.llvmValue, addressLocation)
		}
		val initialStaticValues = LinkedList<LlvmValue>()
		initialStaticValues.add(constantCount)
		initialStaticValues.add(constructor.createNullPointer(constructor.voidType))
		initialStaticValues.add(constructor.createNullPointer(constructor.voidType))
		initialStaticValues.add(propertyCount)
		initialStaticValues.add(constructor.createNullPointer(constructor.voidType))
		initialStaticValues.add(constructor.createNullPointer(constructor.voidType))
		initialStaticValues.add(functionCount)
		initialStaticValues.add(constructor.createNullPointer(constructor.voidType))
		initialStaticValues.add(constructor.createNullPointer(constructor.voidType))
		llvmClassDefinitionAddress = constructor.buildGlobal("${name}_ClassDefinition", context.classDefinitionStruct, constructor.buildConstantStruct(context.classDefinitionStruct, initialStaticValues))
		val constantIdArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.CONSTANT_ID_ARRAY_PROPERTY_INDEX, "constantIdArray")
		val constantOffsetArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.CONSTANT_OFFSET_ARRAY_PROPERTY_INDEX, "constantOffsetArray")
		val propertyIdArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.PROPERTY_ID_ARRAY_PROPERTY_INDEX, "propertyIdArray")
		val propertyOffsetArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.PROPERTY_OFFSET_ARRAY_PROPERTY_INDEX, "propertyOffsetArray")
		val functionIdArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.FUNCTION_ID_ARRAY_PROPERTY_INDEX, "functionIdArray")
		val functionAddressArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.FUNCTION_ADDRESS_ARRAY_PROPERTY_INDEX, "functionAddressArray")
		constructor.buildStore(constantIdArrayAddress, constantIdArrayAddressLocation)
		constructor.buildStore(constantOffsetArrayAddress, constantOffsetArrayAddressLocation)
		constructor.buildStore(propertyIdArrayAddress, propertyIdArrayAddressLocation)
		constructor.buildStore(propertyOffsetArrayAddress, propertyOffsetArrayAddressLocation)
		constructor.buildStore(functionIdArrayAddress, functionIdArrayAddressLocation)
		constructor.buildStore(functionAddressArrayAddress, functionAddressArrayAddressLocation)
		if(this !is Object) {
			val values = LinkedList<LlvmValue>()
			values.add(llvmClassDefinitionAddress)
			for(constant in constants) {
				val value = constant.value?.getLlvmValue(constructor)
				values.add(value ?: constructor.createNullPointer(constructor.voidType))
			}
			staticValueDeclaration.llvmLocation = constructor.buildGlobal("${name}_StaticObject", llvmStaticType, constructor.buildConstantStruct(llvmStaticType, values))
		}
		constructor.buildReturn()
	}

	override fun toString(): String {
		if(superType == null || SpecialType.ANY.matches(superType))
			return name
		return "$name: $superType"
	}
}
