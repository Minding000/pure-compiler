package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.Try as TrySyntaxTree

class Try(override val source: TrySyntaxTree, scope: Scope, val expression: Value, val isOptional: Boolean): Value(source, scope) {

	init {
		addUnits(expression)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		expression.type?.let { expressionType ->
			type = if(isOptional)
				OptionalType(source, scope, expressionType)
			else
				expressionType
		}
	}
}
