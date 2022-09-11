package linting.semantic_model.access

import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import parsing.syntax_tree.access.InstanceAccess

class InstanceAccess(override val source: InstanceAccess, val instance: VariableValue): Value(source) {

	init {
		units.add(instance)
	}
}