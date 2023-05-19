package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.YieldStatement as YieldStatementSyntaxTree

class YieldStatement(override val source: YieldStatementSyntaxTree, scope: Scope, val key: Value?, val value: Value): Value(source, scope) {

	init {
		addSemanticModels(key, value)
	}
}
