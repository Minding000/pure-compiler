package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.control_flow.IfStatement as SemanticIfStatementModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import source_structure.Position
import util.indent

class IfStatement(private val condition: ValueElement, private val positiveBranch: Element,
				  private val negativeBranch: Element?, start: Position, end: Position): Element(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticIfStatementModel {
		return SemanticIfStatementModel(this, condition.concretize(linter, scope),
			positiveBranch.concretize(linter, scope), negativeBranch?.concretize(linter, scope))
	}

	override fun toString(): String {
		var stringRepresentation = "If [ $condition ] {${"\n$positiveBranch".indent()}\n}"
		if(negativeBranch != null)
			stringRepresentation += " Else {${"\n$negativeBranch".indent()}\n}"
		return stringRepresentation
	}
}
