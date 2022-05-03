package linter.elements.values

import linter.elements.general.Unit
import linter.scopes.TypeScope
import parsing.ast.general.Element

open class TypeDefinition(open val source: Element, val name: String, val scope: TypeScope, val superType: Unit?,
						  val isGeneric: Boolean): Unit() {

	init {
		if(superType != null)
			units.add(superType)
	}
}