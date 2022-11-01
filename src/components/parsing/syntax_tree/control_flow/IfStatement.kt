package components.parsing.syntax_tree.control_flow

import components.linting.Linter
import components.linting.semantic_model.control_flow.IfStatement as SemanticIfStatementModel
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.ValueElement
import source_structure.Position
import util.indent

class IfStatement(private val condition: ValueElement, private val trueBranch: Element, private val falseBranch: Element?,
				  start: Position, end: Position): Element(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticIfStatementModel {
		return SemanticIfStatementModel(this, condition.concretize(linter, scope),
			trueBranch.concretize(linter, scope), falseBranch?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "If [ $condition ] {${"\n$trueBranch".indent()}\n}${if(falseBranch == null) "" else " Else {${"\n$falseBranch".indent()}\n}"}"
	}
}
