package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.AndUnionType
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.InterfaceMember
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
		val typeSubstitutions = typeSubstitutions.toMutableMap()
		val placeholders = scope.getGenericTypeDefinitions()
		for(parameterIndex in placeholders.indices) {
			val placeholder = placeholders[parameterIndex]
			val typeParameter = typeParameters.getOrNull(parameterIndex) ?: break
			typeSubstitutions[placeholder] = typeParameter
		}
		withTypeSubstitutions(typeSubstitutions) { specificTypeDefinition ->
			specificTypeDefinition.baseDefinition = this
			onCompletion(specificTypeDefinition)
		}
	}

	fun getComputedSuperType(): Type? {
		superType?.determineTypes()
		return superType
	}

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

	override fun toString(): String {
		if(superType == null || SpecialType.ANY.matches(superType))
			return name
		return "$name: $superType"
	}
}
