package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import util.indent
import components.semantic_analysis.semantic_model.control_flow.Case as SemanticCaseModel

class Case(private val condition: ValueElement, private val result: Element): Element(condition.start, result.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticCaseModel {
		return SemanticCaseModel(this, scope, condition.concretize(linter, scope), result.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Case [ $condition ] {${"\n$result".indent()}\n}"
	}
}
