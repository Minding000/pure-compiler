package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class InstanceList(start: Position, private val instances: List<Instance>): MetaElement(start, instances.last().end) {

	override fun concretize(scope: MutableScope, units: MutableList<Unit>) {
		for(instance in instances)
			instance.concretize(scope, units)
	}

	override fun toString(): String {
		return "InstanceList {${instances.toLines().indent()}\n}"
	}
}
