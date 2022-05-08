package linter.elements.values

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.Scope
import linter.scopes.TypeScope
import parsing.ast.general.Element

open class TypeDefinition(open val source: Element, val name: String, val scope: TypeScope, val superType: Unit?,
						  val isGeneric: Boolean): Unit() {

	init {
		if(superType != null)
			units.add(superType)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, this.scope)
	}
}