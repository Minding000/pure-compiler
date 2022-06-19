package linter.elements.general

import linter.Linter
import linter.scopes.MutableScope
import linter.scopes.Scope
import java.util.*

abstract class Unit {
	val units = LinkedList<Unit>()

	open fun linkTypes(linter: Linter, scope: Scope) {
		for(unit in units)
			unit.linkTypes(linter, scope)
	}

	open fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		for(unit in units)
			unit.linkPropertyParameters(linter, scope)
	}

	open fun linkReferences(linter: Linter, scope: Scope) {
		for(unit in units)
			unit.linkReferences(linter, scope)
	}

	open fun validate(linter: Linter) {
		for(unit in units)
			unit.validate(linter)
	}

//	abstract fun compile(context: BuildContext): Pointer?
}