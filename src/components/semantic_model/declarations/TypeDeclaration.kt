package components.semantic_model.declarations

import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.*
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.declaration.*
import logger.issues.modifiers.NoParentToBindTo
import logger.issues.resolution.LiteralTypeNotFound
import java.util.*
import components.code_generation.llvm.models.declarations.TypeDeclaration as TypeDeclarationUnit

abstract class TypeDeclaration(override val source: SyntaxTreeNode, val name: String, override val scope: TypeScope,
							   val explicitParentType: ObjectType? = null, val superType: Type? = null,
							   val members: MutableList<SemanticModel> = mutableListOf(), val isBound: Boolean = false):
	SemanticModel(source, scope) {
	open val isDefinition = true
	override var parent: SemanticModel?
		get() = super.parent
		set(value) {
			super.parent = value
			parentTypeDeclaration = value as? TypeDeclaration
		}
	var parentTypeDeclaration: TypeDeclaration? = null
	private var hasCircularInheritance = false
	private var hasDeterminedTypes = false
	lateinit var staticValueDeclaration: ValueDeclaration
	lateinit var unit: TypeDeclarationUnit

	init {
		addSemanticModels(explicitParentType, superType)
		addSemanticModels(members)
		for(member in members) {
			if(member is InterfaceMember)
				member.parentTypeDeclaration = this
		}
	}

	fun getLinkedSuperType(): Type? {
		superType?.determineTypes()
		return superType
	}

	fun getAllInitializers(): List<InitializerDefinition> {
		if(hasCircularInheritance)
			return emptyList()
		val allInitializers = LinkedList<InitializerDefinition>()
		allInitializers.addAll(scope.initializers)
		val superScope = scope.superScope ?: return allInitializers
		allInitializers.addAll(superScope.getAllInitializers())
		return allInitializers
	}

	open fun getValueDeclaration(): ValueDeclaration? = null

	override fun determineTypes() {
		if(hasDeterminedTypes)
			return
		hasDeterminedTypes = true
		explicitParentType?.determineTypes()
		if(explicitParentType != null) {
			if(parentTypeDeclaration == null) {
				parentTypeDeclaration = explicitParentType.getTypeDeclaration()
			} else {
				context.addIssue(ExplicitParentOnScopedTypeDefinition(explicitParentType.source))
			}
		}
		// Quick fix: Loading initializers first - bigger resolution rework required
		explicitParentType?.determineTypes()
		superType?.determineTypes()
		if(superType != null && inheritsFrom(this)) {
			hasCircularInheritance = true
			context.addIssue(CircularInheritance(superType.source))
		}
		for(semanticModel in semanticModels)
			if(semanticModel is InitializerDefinition)
				semanticModel.determineSignatureTypes()
		for(semanticModel in semanticModels)
			if(semanticModel is InitializerDefinition)
				semanticModel.determineTypes()
		if(isDefinition) {
			if(members.any { member -> member is Instance })
				addStaticProperties()
			if(scope.initializers.isEmpty())
				addDefaultInitializer()
		}
		for(semanticModel in semanticModels)
			if(semanticModel !is InitializerDefinition && semanticModel !== explicitParentType)
				semanticModel.determineTypes()
		scope.ensureUniqueInitializerSignatures()
		scope.inheritSignatures()
	}

	//TODO how is this supposed to interact with inheritance?
	// example: Color.RED - TranslucentColor.RED?
	private fun addStaticProperties() {
		val stringType = LiteralType(source, scope, SpecialType.STRING)
		val selfType = SelfType(this)
		val mapTypeDeclaration = context.nativeRegistry.specialTypeScopes[SpecialType.MAP]?.getTypeDeclaration("Map")
		if(mapTypeDeclaration == null) {
			context.addIssue(LiteralTypeNotFound(source, "Map"))
			return
		}
		val type = ObjectType(listOf(stringType, selfType), mapTypeDeclaration)
		val property = PropertyDeclaration(source, scope, "staticInstances", type, null, true)
		members.add(property)
		property.parentTypeDeclaration = this
		addSemanticModels(property)
		scope.addValueDeclaration(property)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		for(semanticModel in semanticModels) {
			if(semanticModel !is InitializerDefinition)
				semanticModel.analyseDataFlow(tracker)
		}
		if(!hasCircularInheritance) {
			for(initializer in scope.initializers)
				initializer.analyseDataFlow(tracker)
		}
	}

	override fun validate() {
		super.validate()
		if(isBound && parentTypeDeclaration == null)
			context.addIssue(NoParentToBindTo(source))
		if(isDefinition) {
			if((this as? Class)?.isAbstract != true && !hasCircularInheritance)
				ensureAbstractSuperMembersImplemented()
			ensureSpecificMembersOverridden()
			validateMonomorphicSuperMember()
		}
	}

	private fun ensureAbstractSuperMembersImplemented() {
		val missingOverrides = LinkedHashMap<TypeDeclaration, LinkedList<MemberDeclaration>>()
		for((unimplementedMember) in getUnimplementedAbstractSuperMemberDeclarations()) {
			val parentDefinition = unimplementedMember.parentTypeDeclaration
				?: throw CompilerError(unimplementedMember.source, "Member is missing parent definition.")
			val missingOverridesFromType = missingOverrides.getOrPut(parentDefinition) { LinkedList() }
			missingOverridesFromType.add(unimplementedMember)
		}
		if(missingOverrides.isEmpty())
			return
		context.addIssue(MissingImplementations(this, missingOverrides))
	}

	fun getUnimplementedAbstractSuperMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> {
		val abstractSuperMembers = superType?.getPotentiallyUnimplementedAbstractMemberDeclarations() ?: return emptyList()
		return abstractSuperMembers.filter { (abstractSuperMember, typeSubstitutions) ->
			!implements(abstractSuperMember, typeSubstitutions)
		}
	}

	fun implements(abstractMember: MemberDeclaration, typeSubstitutions: Map<TypeDeclaration, Type>): Boolean {
		return scope.memberDeclarations.any { memberDeclaration ->
			if(memberDeclaration == abstractMember)
				return false
			return@any if(memberDeclaration is PropertyDeclaration && abstractMember is PropertyDeclaration)
				memberDeclaration.name == abstractMember.name
			else if(memberDeclaration is FunctionImplementation && abstractMember is FunctionImplementation)
				memberDeclaration.fulfillsInheritanceRequirementsOf(abstractMember, typeSubstitutions)
			else if(memberDeclaration is InitializerDefinition && abstractMember is InitializerDefinition)
				memberDeclaration.fulfillsInheritanceRequirementsOf(abstractMember, typeSubstitutions)
			else if(memberDeclaration is Instance && abstractMember is Instance)
				memberDeclaration.name == abstractMember.name
			else false
		} || superType?.implements(abstractMember, typeSubstitutions) ?: false
	}

	private fun ensureSpecificMembersOverridden() {
		val missingOverrides = LinkedHashMap<TypeDeclaration, LinkedList<MemberDeclaration>>()
		for((unimplementedMember) in getUnoverriddenSpecificSuperMemberDeclarations()) {
			val parentDefinition = unimplementedMember.parentTypeDeclaration
				?: throw CompilerError(unimplementedMember.source, "Member is missing parent definition.")
			val missingOverridesFromType = missingOverrides.getOrPut(parentDefinition) { LinkedList() }
			missingOverridesFromType.add(unimplementedMember)
		}
		if(missingOverrides.isEmpty())
			return
		context.addIssue(MissingSpecificOverrides(this, missingOverrides))
	}

	private fun getUnoverriddenSpecificSuperMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> {
		val specificSuperMembers = superType?.getSpecificMemberDeclarations() ?: return emptyList()
		return specificSuperMembers.filter { (specificSuperMember, typeSubstitutions) ->
			!overrides(specificSuperMember, typeSubstitutions)
		}
	}

	private fun overrides(specificMember: MemberDeclaration, typeSubstitutions: Map<TypeDeclaration, Type>): Boolean {
		return scope.memberDeclarations.any { memberDeclaration ->
			if(memberDeclaration == specificMember)
				return false
			if(memberDeclaration !is FunctionImplementation || specificMember !is FunctionImplementation)
				return@any false
			return@any memberDeclaration.signature.fulfillsInheritanceRequirementsOf(
				specificMember.signature.withTypeSubstitutions(typeSubstitutions))
		}
	}

	private fun validateMonomorphicSuperMember() {
		val superTypeContainsMonomorphicMemberImplementations = getDirectSuperTypes().any { superType ->
			val classDeclaration = superType.getTypeDeclaration() as? Class ?: return@any false
			return@any !classDeclaration.isAbstract && classDeclaration.containsMonomorphicMemberImplementation()
		}
		if(superTypeContainsMonomorphicMemberImplementations)
			context.addIssue(MonomorphicInheritance(source))
	}

	private fun addDefaultInitializer() {
		val defaultInitializer = InitializerDefinition(source, BlockScope(scope))
		defaultInitializer.determineSignatureTypes()
		addSemanticModels(defaultInitializer)
		members.add(defaultInitializer)
	}

	private fun inheritsFrom(definition: TypeDeclaration): Boolean {
		for(superType in getDirectSuperTypes()) {
			superType.determineTypes()
			val superTypeDeclaration = superType.getTypeDeclaration()
			if(superTypeDeclaration == definition)
				return true
			if(superTypeDeclaration?.inheritsFrom(definition) == true)
				return true
		}
		return false
	}

	fun getAllSuperTypes(): List<ObjectType> {
		val superTypes = LinkedList<ObjectType>()
		for(superType in getDirectSuperTypes()) {
			superTypes.add(superType)
			superTypes.addAll(superType.getTypeDeclaration()?.getAllSuperTypes() ?: continue)
		}
		return superTypes
	}

	fun getDirectSuperTypes(): List<ObjectType> {
		return when(val superType = superType) {
			is ObjectType -> listOf(superType)
			is AndUnionType -> superType.types.filterIsInstance<ObjectType>()
			else -> emptyList()
		}
	}

	open fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		determineTypes()
		return scope.getConversionsFrom(sourceType)
	}

	fun getGenericTypes(): List<ObjectType> {
		return scope.getGenericTypeDeclarations().map { genericTypeDeclaration -> ObjectType(genericTypeDeclaration) }
	}

	//TODO fix: it's not clear that Identifiable inherits from Any when running without STD lib
	fun acceptsSubstituteType(substituteType: Type): Boolean {
		if(superType == null)
			return false
		if(SpecialType.ANY.matches(superType))
			return true
		return superType.accepts(substituteType)
	}

	override fun equals(other: Any?): Boolean {
		if(other !is TypeDeclaration)
			return false
		return source == other.source
	}

	override fun hashCode(): Int {
		return source.hashCode()
	}

	fun isLlvmPrimitive(): Boolean {
		return SpecialType.BOOLEAN.matches(this)
			|| SpecialType.BYTE.matches(this)
			|| SpecialType.INTEGER.matches(this)
			|| SpecialType.FLOAT.matches(this)
	}

	override fun toString(): String {
		if(superType == null || SpecialType.ANY.matches(superType))
			return name
		return "$name: $superType"
	}

	fun getFullName(): String {
		val parentTypeDeclaration = parentTypeDeclaration ?: return name
		return "${parentTypeDeclaration.getFullName()}.$name"

	}
}
