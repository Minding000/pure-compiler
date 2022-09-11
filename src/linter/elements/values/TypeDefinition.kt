package linter.elements.values

import linter.Linter
import linter.elements.general.Unit
import linter.elements.literals.ObjectType
import linter.elements.literals.Type
import linter.scopes.MutableScope
import linter.scopes.Scope
import linter.scopes.TypeScope
import parsing.ast.general.Element

abstract class TypeDefinition(open val source: Element, val name: String, val scope: TypeScope, val superType: Type?):
	Unit() {

	init {
		if(superType != null)
			units.add(superType)
	}

	abstract fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): TypeDefinition

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
		this.scope.ensureUniqueSignatures(linter)
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