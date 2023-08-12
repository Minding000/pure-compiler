package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.WhileGenerator as WhileGeneratorSyntaxTree

class WhileGenerator(override val source: WhileGeneratorSyntaxTree, scope: Scope, val condition: Value, val isPostCondition: Boolean):
	SemanticModel(source, scope) { //TODO test post condition compilation

	init {
		addSemanticModels(condition)
	}
}
