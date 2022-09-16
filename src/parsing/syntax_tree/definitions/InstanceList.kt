package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class InstanceList(start: Position, private val instances: List<Instance>): MetaElement(start, instances.last().end) {

	override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		for(instance in instances)
			instance.concretize(linter, scope, units)
	}

	override fun toString(): String {
		return "InstanceList {${instances.toLines().indent()}\n}"
	}
}