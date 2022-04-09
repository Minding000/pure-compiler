package linter.elements.values

import linter.elements.general.Unit

class TypeDefinition(val name: String, val superType: Unit? = null, val isGeneric: Boolean = false): Unit() {

	init {
		if(superType != null)
			units.add(superType)
	}
}