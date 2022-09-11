package parsing.syntax_tree.operations

import linting.Linter
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import linting.semantic_model.operations.Assignment
import parsing.syntax_tree.general.ValueElement
import util.concretizeValues
import util.indent
import util.toLines

class Assignment(private val targets: List<ValueElement>, val source: ValueElement):
	Element(targets.first().start, source.end) {

	override fun concretize(linter: Linter, scope: MutableScope): Assignment {
		return Assignment(this, targets.concretizeValues(linter, scope), source.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Assignment {${"${targets.toLines()}\n= $source".indent()}\n}"
	}
}