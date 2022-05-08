package linter.elements.general

import linter.Linter
import linter.elements.literals.Type
import linter.scopes.Scope
import java.util.*

open class Unit(var type: Type? = null) {
	val units = LinkedList<Unit>()

	open fun linkTypes(linter: Linter, scope: Scope) {
		for(unit in units)
			unit.linkTypes(linter, scope)
	}

	open fun linkReferences(linter: Linter, scope: Scope) {
		for(unit in units)
			unit.linkReferences(linter, scope)
	}

	open fun validate(linter: Linter) {
		for(unit in units)
			unit.validate(linter)
	}
}