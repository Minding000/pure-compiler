package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines

class InstanceList(start: Position, private val instances: List<Instance>): MetaSyntaxTreeNode(start, instances.last().end) {

	override fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>) {
		for(instance in instances)
			instance.toSemanticModel(scope, semanticModels)
	}

	override fun toString(): String {
		return "InstanceList {${instances.toLines().indent()}\n}"
	}
}
