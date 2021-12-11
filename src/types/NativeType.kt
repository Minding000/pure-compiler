package types

import elements.definitions.ClassDefinition

class NativeType(override val name: String): Type {

	override fun getClass(): ClassDefinition {
		TODO("Not yet implemented")
	}

	override fun toString(): String {
		return "NativeType { $name }"
	}
}