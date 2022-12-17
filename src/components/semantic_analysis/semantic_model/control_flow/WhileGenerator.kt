package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.WhileGenerator as WhileGeneratorSyntaxTree

class WhileGenerator(override val source: WhileGeneratorSyntaxTree, val condition: Value, val isPostCondition: Boolean):
	Unit(source) {

	init {
		addUnits(condition)
	}
}
