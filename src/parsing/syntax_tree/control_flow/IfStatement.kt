package parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.IfStatement
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.ValueElement
import source_structure.Position
import util.indent

class IfStatement(private val condition: ValueElement, private val trueBranch: Element, private val falseBranch: Element?,
				  start: Position, end: Position): Element(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): IfStatement {
		return IfStatement(this, condition.concretize(linter, scope), trueBranch.concretize(linter, scope),
			falseBranch?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "If [ $condition ] {${"\n$trueBranch".indent()}\n}${if(falseBranch == null) "" else " Else {${"\n$falseBranch".indent()}\n}"}"
	}
}
