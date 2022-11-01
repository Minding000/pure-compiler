package components.syntax_parser.syntax_tree.operations

import components.linting.Linter
import components.linting.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.linting.semantic_model.operations.Assignment as SemanticAssignmentModel
import components.syntax_parser.syntax_tree.general.ValueElement
import util.concretizeValues
import util.indent
import util.toLines

class Assignment(private val targets: List<ValueElement>, val source: ValueElement):
	Element(targets.first().start, source.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticAssignmentModel {
		return SemanticAssignmentModel(this, targets.concretizeValues(linter, scope),
			source.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Assignment {${"${targets.toLines()}\n= $source".indent()}\n}"
	}
}
