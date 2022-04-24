package linter.elements.values

import linter.elements.general.Unit
import parsing.ast.general.Element

open class TypeDefinition(open val source: Element, val name: String, val superType: Unit?, val isGeneric: Boolean): Unit() {

	init {
		if(superType != null)
			units.add(superType)
	}
}