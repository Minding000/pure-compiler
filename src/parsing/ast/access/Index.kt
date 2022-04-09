package parsing.ast.access

import linter.Linter
import linter.elements.access.Index
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import source_structure.Position
import util.concretize
import util.indent
import util.toLines
import java.util.*

class Index(private val target: Element, private val indices: List<Element>, end: Position): Element(target.start, end) {

	override fun concretize(linter: Linter, scope: Scope): Index {
		return Index(this, target.concretize(linter, scope), indices.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Index [ $target ] {${indices.toLines().indent()}\n}"
	}
}