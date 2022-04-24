package parsing.ast.operations

import linter.Linter
import linter.scopes.Scope
import parsing.ast.general.Element
import linter.elements.operations.Assignment
import util.concretize
import util.indent
import util.toLines

class Assignment(val targets: List<Element>, val source: Element): Element(targets.first().start, source.end) {

	override fun concretize(linter: Linter, scope: Scope): Assignment {
		return Assignment(this, targets.concretize(linter, scope), source.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Assignment {${"${targets.toLines()}\n= $source".indent()}\n}"
	}
}