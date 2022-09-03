package linter.elements.values

import linter.Linter
import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.scopes.MutableScope
import linter.scopes.Scope
import linter.scopes.TypeScope
import parsing.ast.general.Element

open class TypeDefinition(open val source: Element, val name: String, val scope: TypeScope, val superType: Type?,
						  val isGeneric: Boolean = false): Unit() {
	private val specificDefinitions = HashMap<Map<Type, Type>, TypeDefinition>()

	init {
		if(superType != null)
			units.add(superType)
	}

	fun withTypeSubstitutions(typeSubstitution: Map<Type, Type>): TypeDefinition {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			val superType = superType?.withTypeSubstitutions(typeSubstitution)
			definition = TypeDefinition(source, name, scope.withTypeSubstitutions(typeSubstitution, superType?.scope), superType)
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		super.linkPropertyParameters(linter, this.scope)
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