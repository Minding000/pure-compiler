package components.linting.semantic_model.control_flow

import components.linting.Linter
import components.linting.semantic_model.types.OptionalType
import components.linting.semantic_model.scopes.Scope
import components.linting.semantic_model.values.Value
import components.parsing.syntax_tree.control_flow.Try as TrySyntaxTree

class Try(override val source: TrySyntaxTree, val expression: Value, val isOptional: Boolean): Value(source) {

	init {
		units.add(expression)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		expression.type?.let { expressionType ->
			type = if(isOptional)
				OptionalType(source, expressionType)
			else
				expressionType
		}
	}
}
