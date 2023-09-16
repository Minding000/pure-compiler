package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines

class GenericsDeclaration(start: Position, private val elements: List<Parameter>): MetaSyntaxTreeNode(start, elements.last().end) {

	override fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>) {
		for(element in elements)
			semanticModels.add(element.toSemanticGenericParameterModel(scope))
	}

	override fun toString(): String {
		return "GenericsDeclaration {${elements.toLines().indent()}\n}"
	}
}
