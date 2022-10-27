package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.Type
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.scopes.Scope
import linting.semantic_model.scopes.TypeScope
import parsing.syntax_tree.general.Element

abstract class TypeDefinition(override val source: Element, val name: String, val scope: TypeScope,
							  val superType: Type?): Unit(source) {
	var baseDefinition: TypeDefinition? = null

	init {
		if(superType != null)
			units.add(superType)
	}

	open fun register(linter: Linter, parentScope: MutableScope) {}

	abstract fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): TypeDefinition

	fun withTypeParameters(typeParameters: List<Type>): TypeDefinition {
		baseDefinition?.let { baseDefinition ->
			return baseDefinition.withTypeParameters(typeParameters)
		}
		val placeholders = scope.getGenericTypes()
		val typeSubstitutions = HashMap<ObjectType, Type>()
		for(parameterIndex in placeholders.indices) {
			val placeholder = placeholders[parameterIndex]
			val typeParameter = typeParameters.getOrNull(parameterIndex) ?: break
			typeSubstitutions[placeholder] = typeParameter
		}
		val specificTypeDefinition = withTypeSubstitutions(typeSubstitutions)
		specificTypeDefinition.baseDefinition = this
		units.add(specificTypeDefinition)
		return specificTypeDefinition
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
		this.scope.inheritSignatures()
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		super.linkPropertyParameters(linter, this.scope)
		this.scope.ensureUniqueInitializerSignatures(linter)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
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
		if(superType == null)
			return name
		return "$name: $superType"
	}
}
