package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.Case as CaseSyntaxTree

class Case(override val source: CaseSyntaxTree, scope: Scope, val condition: Value, val result: SemanticModel): SemanticModel(source, scope) {

	init {
		addSemanticModels(condition, result)
	}
}
