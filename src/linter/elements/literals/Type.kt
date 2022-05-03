package linter.elements.literals

import linter.elements.general.Unit
import linter.scopes.InterfaceScope

abstract class Type: Unit() {
	val scope = InterfaceScope()

	abstract fun accepts(sourceType: Type): Boolean
	abstract fun isAssignableTo(targetType: Type): Boolean
}