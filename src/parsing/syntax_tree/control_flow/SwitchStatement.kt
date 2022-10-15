package parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.SwitchStatement
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.ValueElement
import source_structure.Position
import util.indent
import util.toLines
import java.util.*

class SwitchStatement(private val subject: ValueElement, private val cases: LinkedList<Case>,
					  private val elseBranch: Element?, start: Position, end: Position): Element(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SwitchStatement {
		val cases = LinkedList<linting.semantic_model.control_flow.Case>()
		for(case in this.cases)
			cases.add(case.concretize(linter, scope))
		return SwitchStatement(this, subject.concretize(linter, scope), cases, elseBranch?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Switch [ $subject ] {${cases.toLines().indent()}\n}${if(elseBranch == null) "" else " Else {${"\n$elseBranch".indent()}\n}"}"
	}
}
