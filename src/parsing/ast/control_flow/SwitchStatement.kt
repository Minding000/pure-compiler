package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.SwitchStatement
import linter.scopes.MutableScope
import parsing.ast.general.Element
import source_structure.Position
import util.indent
import util.toLines
import java.util.*

class SwitchStatement(private val subject: Element, private val cases: LinkedList<Case>,
					  private val elseBranch: Element?, start: Position, end: Position): Element(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SwitchStatement {
		val cases = LinkedList<linter.elements.control_flow.Case>()
		for(case in this.cases)
			cases.add(case.concretize(linter, scope))
		return SwitchStatement(this, subject.concretize(linter, scope), cases, elseBranch?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Switch [ $subject ] {${cases.toLines().indent()}\n}${if(elseBranch == null) "" else " Else {${"\n$elseBranch".indent()}\n}"}"
	}
}