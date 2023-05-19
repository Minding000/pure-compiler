package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import source_structure.Position
import util.indent
import components.semantic_analysis.semantic_model.control_flow.WhileGenerator as SemanticWhileGeneratorModel

class WhileGenerator(start: Position, private val condition: ValueSyntaxTreeNode, private val isPostCondition: Boolean):
	SyntaxTreeNode(start, condition.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticWhileGeneratorModel {
		return SemanticWhileGeneratorModel(this, scope, condition.toSemanticModel(scope), isPostCondition)
	}

	override fun toString(): String {
		return "WhileGenerator [${if(isPostCondition) "post" else "pre"}] {${"\n$condition".indent()}\n}"
	}
}
