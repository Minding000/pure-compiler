package components.parsing.syntax_tree.definitions

import components.linting.Linter
import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class GenericsDeclaration(start: Position, private val elements: List<Parameter>):
	MetaElement(start, elements.last().end) {

	override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		for(element in elements)
			units.add(element.concretizeAsGenericParameter(linter, scope))
	}

	override fun toString(): String {
		return "GenericsDeclaration {${elements.toLines().indent()}\n}"
	}
}
