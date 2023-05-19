package components.syntax_parser.syntax_tree.operations

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import util.concretizeValues
import util.indent
import util.toLines
import components.semantic_analysis.semantic_model.operations.Assignment as SemanticAssignmentModel

class Assignment(private val targets: List<ValueElement>, val source: ValueElement): Element(targets.first().start, source.end) {

	override fun concretize(scope: MutableScope): SemanticAssignmentModel {
		return SemanticAssignmentModel(this, scope, targets.concretizeValues(scope), source.concretize(scope))
	}

	override fun toString(): String {
		return "Assignment {${"${targets.toLines()}\n= $source".indent()}\n}"
	}
}
