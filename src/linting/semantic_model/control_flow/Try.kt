package linting.semantic_model.control_flow

import linting.Linter
import linting.semantic_model.types.OptionalType
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
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
