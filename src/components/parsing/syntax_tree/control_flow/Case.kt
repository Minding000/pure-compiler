package components.parsing.syntax_tree.control_flow

import components.linting.Linter
import components.linting.semantic_model.control_flow.Case as SemanticCaseModel
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.ValueElement
import util.indent

class Case(private val condition: ValueElement, private val result: Element): Element(condition.start, result.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticCaseModel {
		return SemanticCaseModel(this, condition.concretize(linter, scope), result.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Case [ $condition ] {${"\n$result".indent()}\n}"
	}
}
