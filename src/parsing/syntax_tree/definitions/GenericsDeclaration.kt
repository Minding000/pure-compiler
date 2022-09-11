package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class GenericsDeclaration(start: Position, private val elements: List<GenericsListElement>): MetaElement(start, elements.last().end) {

	override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		for(element in elements)
			element.concretize(linter, scope, units)
	}

	override fun toString(): String {
		return "GenericsDeclaration {${elements.toLines().indent()}\n}"
	}
}