package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.IfStatement
import linter.scopes.MutableScope
import parsing.ast.general.Element
import source_structure.Position
import util.indent

class IfStatement(private val condition: Element, private val trueBranch: Element, private val falseBranch: Element?, start: Position, end: Position): Element(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): IfStatement {
		return IfStatement(this, trueBranch.concretize(linter, scope), falseBranch?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "If [ $condition ] {${"\n$trueBranch".indent()}\n}${if(falseBranch == null) "" else " Else {${"\n$falseBranch".indent()}\n}"}"
	}
}