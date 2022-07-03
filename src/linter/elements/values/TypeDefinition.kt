package linter.elements.values

import linter.Linter
import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.scopes.MutableScope
import linter.scopes.Scope
import linter.scopes.TypeScope
import parsing.ast.general.Element

open class TypeDefinition(open val source: Element, val name: String, val scope: TypeScope, val superType: Type?,
						  val isGeneric: Boolean): Unit() {

	init {
		if(superType != null)
			units.add(superType)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		super.linkPropertyParameters(linter, this.scope)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, this.scope)
	}
}