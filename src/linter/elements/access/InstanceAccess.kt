package linter.elements.access

import linter.elements.values.Value
import linter.elements.values.VariableValue
import parsing.ast.access.InstanceAccess

class InstanceAccess(val source: InstanceAccess, val instance: VariableValue): Value() {

	init {
		units.add(instance)
	}
}