package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import source_structure.Position
import util.indent
import components.semantic_analysis.semantic_model.control_flow.WhileGenerator as SemanticWhileGeneratorModel

class WhileGenerator(start: Position, private val condition: ValueElement, private val isPostCondition: Boolean):
	Element(start, condition.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticWhileGeneratorModel {
		return SemanticWhileGeneratorModel(this, scope, condition.concretize(linter, scope), isPostCondition)
	}

	override fun toString(): String {
		return "WhileGenerator [${if(isPostCondition) "post" else "pre"}] {${"\n$condition".indent()}\n}"
	}
}
