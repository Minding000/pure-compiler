package elements.identifier

import scopes.Scope

interface Identifier {
	val name: String
	val parentScope: Scope

	fun serializeDeclarationPosition(): String
}