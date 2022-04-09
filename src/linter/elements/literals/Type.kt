package linter.elements.literals

import linter.elements.general.Unit

abstract class Type: Unit() {

	abstract fun accepts(sourceType: Type): Boolean
	abstract fun isAssignableTo(targetType: Type): Boolean
}