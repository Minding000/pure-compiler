package parsing.ast.operations

import linter.Linter
import linter.scopes.MutableScope
import parsing.ast.general.Element
import linter.elements.operations.Assignment
import parsing.ast.general.ValueElement
import util.concretizeValues
import util.indent
import util.toLines

class Assignment(val targets: List<ValueElement>, val source: ValueElement): Element(targets.first().start, source.end) {

	override fun concretize(linter: Linter, scope: MutableScope): Assignment {
		return Assignment(this, targets.concretizeValues(linter, scope), source.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Assignment {${"${targets.toLines()}\n= $source".indent()}\n}"
	}
}