package components.semantic_analysis.semantic_model.definitions

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.Context
import components.semantic_analysis.semantic_model.context.IdentityMap
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
	lateinit var llvmType: LlvmType
	lateinit var llvmClassDefinitionAddress: LlvmValue
	lateinit var llvmClassInitializer: LlvmValue
	lateinit var llvmClassInitializerType: LlvmType

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

	private fun getDirectSuperTypes(): List<ObjectType> {
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
			llvmType = constructor.declareStruct("${name}_ClassStruct")
		}
		super.declare(constructor)
	}

	override fun define(constructor: LlvmConstructor) {
		if(isDefinition) {
			val flattenedMembers = getFlattenedMembers()
			defineLlvmStruct(constructor, flattenedMembers)
			defineLlvmClassInitializer(constructor, flattenedMembers)
		}
		super.define(constructor)
	}

	fun getFlattenedMembers(): List<MemberDeclaration> {
		val flattenedMembers = LinkedList(scope.memberDeclarations)
		for(superType in getDirectSuperTypes())
			flattenedMembers.addAll(superType.definition?.getFlattenedMembers() ?: emptyList())
		return flattenedMembers
	}

	private fun defineLlvmStruct(constructor: LlvmConstructor, flattenedMembers: List<MemberDeclaration>) {
		val members = LinkedList<LlvmType?>()
		members.add(constructor.createPointerType(context.classDefinitionStruct))
		for(memberDeclaration in flattenedMembers) {
			if(memberDeclaration is ValueDeclaration) {
				members.add(memberDeclaration.type?.getLlvmType(constructor))
			} else {
				// Placeholder
				members.add(constructor.createPointerType(constructor.voidType))
			}
		}
		constructor.defineStruct(llvmType, members)
	}

	private fun defineLlvmClassInitializer(constructor: LlvmConstructor, flattenedMembers: List<MemberDeclaration>) {
		println("$name class initializer:")
		constructor.createAndSelectBlock(llvmClassInitializer, "entrypoint")
		for(typeDefinition in scope.typeDefinitions.values) {
			if(typeDefinition is Object) {
				constructor.buildFunctionCall(typeDefinition.llvmClassInitializerType, typeDefinition.llvmClassInitializer)
			}
		}
		val memberCount = constructor.buildInt32(flattenedMembers.size)
		val memberIdArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, memberCount, "memberIdArray")
		val memberOffsetArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, memberCount, "memberOffsetArray")
		for((memberIndex, memberDeclaration) in flattenedMembers.withIndex()) {
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, memberIdArrayAddress, memberIndexValue, "memberIdLocation")
			if(memberDeclaration is ValueDeclaration) {
				val memberId = context.memberIdentities.register(memberDeclaration.memberIdentifier)
				val structMemberIndex = memberIndex + 1
				val memberOffset = constructor.getMemberOffsetInBytes(llvmType, structMemberIndex)
				println("Mapping member '${memberDeclaration.memberIdentifier}' to ID '$memberId' with offset '$memberOffset'.")
				val offsetLocation = constructor.buildGetArrayElementPointer(context.llvmMemberOffsetType, memberOffsetArrayAddress, memberIndexValue, "memberOffsetLocation")
				val memberIdValue = constructor.buildInt32(memberId)
				val memberOffsetValue = constructor.buildInt32(memberOffset)
				constructor.buildStore(memberIdValue, idLocation)
				constructor.buildStore(memberOffsetValue, offsetLocation)
			} else {
				println("Hiding member '${memberDeclaration.memberIdentifier}' for now.")
				val memberIdValue = constructor.buildInt32(IdentityMap.NULL_ID)
				constructor.buildStore(memberIdValue, idLocation)
			}
		}
		val initialStaticValues = LinkedList<LlvmValue>()
		initialStaticValues.add(memberCount)
		initialStaticValues.add(constructor.createNullPointer(constructor.voidType))
		initialStaticValues.add(constructor.createNullPointer(constructor.voidType))
		llvmClassDefinitionAddress = constructor.buildGlobal("${name}_ClassDefinition", context.classDefinitionStruct, constructor.buildConstantStruct(context.classDefinitionStruct, initialStaticValues))
		val memberIdArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.MEMBER_ID_ARRAY_PROPERTY_INDEX, "memberIdArray")
		val memberOffsetArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.MEMBER_OFFSET_ARRAY_PROPERTY_INDEX, "memberOffsetArray")
		constructor.buildStore(memberIdArrayAddress, memberIdArrayAddressLocation)
		constructor.buildStore(memberOffsetArrayAddress, memberOffsetArrayAddressLocation)
		constructor.buildReturn()
	}

	override fun toString(): String {
		if(superType == null || SpecialType.ANY.matches(superType))
			return name
		return "$name: $superType"
	}
}
