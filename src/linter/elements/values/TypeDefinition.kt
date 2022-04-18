package linter.elements.values

import linter.elements.general.Unit

open class TypeDefinition(val name: String, val superType: Unit?, val isGeneric: Boolean): Unit() {

	init {
		if(superType != null)
			units.add(superType)
	}
}