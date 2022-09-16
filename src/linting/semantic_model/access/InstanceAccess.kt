package linting.semantic_model.access

import linting.Linter
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import parsing.syntax_tree.access.InstanceAccess

class InstanceAccess(override val source: InstanceAccess, val instance: VariableValue): Value(source) {

	init {
		units.add(instance)
	}

	override fun isAssignableTo(targetType: Type?): Boolean {
		return targetType?.scope?.hasInstance(instance.name) ?: false
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		type?.let { type ->
			super.linkValues(linter, type.scope)
		}
	}
}