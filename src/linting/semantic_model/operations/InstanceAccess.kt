package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.types.OptionalType
import linting.semantic_model.types.Type
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import components.parsing.syntax_tree.access.InstanceAccess as InstanceAccessSyntaxTree

class InstanceAccess(override val source: InstanceAccessSyntaxTree, val name: String): Value(source) {

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

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + name.hashCode()
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is InstanceAccess)
			return false
		return name == other.name && super.equals(other)
	}
}
