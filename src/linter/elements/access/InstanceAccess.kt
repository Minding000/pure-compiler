package linter.elements.access

import linter.elements.general.Unit
import linter.elements.values.VariableValue
import parsing.ast.access.InstanceAccess

class InstanceAccess(val source: InstanceAccess, val instance: VariableValue): Unit() {

	init {
		units.add(instance)
	}
}