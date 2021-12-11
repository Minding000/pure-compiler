package types

import elements.definitions.ClassDefinition

interface Type {
	val name: String

	fun getClass(): ClassDefinition
}