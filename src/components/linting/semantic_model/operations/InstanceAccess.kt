package components.linting.semantic_model.operations

import components.linting.Linter
import components.linting.semantic_model.types.OptionalType
import components.linting.semantic_model.types.Type
import components.linting.semantic_model.scopes.Scope
import components.linting.semantic_model.values.Value
import components.syntax_parser.syntax_tree.access.InstanceAccess as InstanceAccessSyntaxTree

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
