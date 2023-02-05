package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import util.linkedListOf
import java.util.*

abstract class TypeDefinition(override val source: Element, val name: String, val scope: TypeScope, val superType: Type?): Unit(source) {
	// Only used in base definition
	private val specificDefinitions = HashMap<Map<TypeDefinition, Type>, TypeDefinition>()
	private val pendingTypeSubstitutions = HashMap<Map<TypeDefinition, Type>, LinkedList<(TypeDefinition) -> kotlin.Unit>>()
	// Only used in specific definition
	var baseDefinition: TypeDefinition? = null

	init {
		addUnits(superType)
	}

	open fun register(linter: Linter, parentScope: MutableScope) {}

	protected abstract fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): TypeDefinition

	fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>, onCompletion: (TypeDefinition) -> kotlin.Unit) {
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

	fun withTypeParameters(typeParameters: List<Type>, onCompletion: (TypeDefinition) -> kotlin.Unit) {
		baseDefinition?.let { baseDefinition ->
			return baseDefinition.withTypeParameters(typeParameters, onCompletion)
		}
		val placeholders = scope.getGenericTypeDefinitions()
		val typeSubstitutions = HashMap<TypeDefinition, Type>()
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

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		super.linkPropertyParameters(linter, this.scope)
		this.scope.ensureUniqueInitializerSignatures(linter)
	}

	override fun resolveGenerics(linter: Linter) {
		super.resolveGenerics(linter)
		this.scope.inheritSignatures()
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		for(member in scope.memberDeclarations) {
			if(member is FunctionImplementation)
				member.analyseDataFlow(linter, tracker)
		}
		for(initializer in scope.initializers)
			initializer.analyseDataFlow(linter, tracker)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if((this as? Class)?.isAbstract != true)
			scope.ensureAbstractSuperMembersImplemented(linter)
	}

	fun acceptsSubstituteType(substituteType: Type): Boolean {
		if(Linter.SpecialType.ANY.matches(superType))
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
		if(superType == null || Linter.SpecialType.ANY.matches(superType))
			return name
		return "$name: $superType"
	}
}
