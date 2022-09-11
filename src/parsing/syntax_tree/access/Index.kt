package parsing.syntax_tree.access

import linting.Linter
import linting.semantic_model.access.Index
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import source_structure.Position
import util.concretizeValues
import util.indent
import util.toLines

class Index(private val target: ValueElement, private val indices: List<ValueElement>, end: Position): ValueElement(target.start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): Index {
		return Index(this, target.concretize(linter, scope), indices.concretizeValues(linter, scope))
	}

	override fun toString(): String {
		return "Index [ $target ] {${indices.toLines().indent()}\n}"
	}
}