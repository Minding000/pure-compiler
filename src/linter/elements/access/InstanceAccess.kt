package linter.elements.access

import linter.elements.values.Value
import linter.elements.values.VariableValue
import parsing.ast.access.InstanceAccess

class InstanceAccess(override val source: InstanceAccess, val instance: VariableValue): Value(source) {

	init {
		units.add(instance)
	}
}