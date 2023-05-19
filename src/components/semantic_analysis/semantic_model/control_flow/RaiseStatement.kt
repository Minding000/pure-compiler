package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.RaiseStatement as RaiseStatementSyntaxTree

class RaiseStatement(override val source: RaiseStatementSyntaxTree, scope: Scope, val value: Value): SemanticModel(source, scope) {
	override val isInterruptingExecution = true

	init {
		addSemanticModels(value)
	}
}
