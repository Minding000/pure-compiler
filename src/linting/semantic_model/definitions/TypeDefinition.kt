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

	init {
		if(superType != null)
			units.add(superType)
	}

	abstract fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): TypeDefinition

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

	override fun toString(): String {
		if(superType == null)
			return name
		return "$name: $superType"
	}
}
