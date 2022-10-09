package linting.semantic_model.access

import linting.Linter
import linting.semantic_model.literals.OptionalType
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import parsing.syntax_tree.access.InstanceAccess

class InstanceAccess(override val source: InstanceAccess, val name: String): Value(source) {

	override fun isAssignableTo(targetType: Type?): Boolean {
		return if(targetType is OptionalType)
			targetType.baseType.scope.hasInstance(name)
		else
			targetType?.scope?.hasInstance(name) ?: false
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		type?.let { type ->
			super.linkValues(linter, type.scope)
		}
	}
}
