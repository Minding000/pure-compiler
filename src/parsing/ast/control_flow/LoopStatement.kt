package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.LoopStatement
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.general.StatementSection
import source_structure.Position

class LoopStatement(start: Position, private val generator: Element?, private val body: StatementSection): Element(start, body.end) {

	override fun concretize(linter: Linter, scope: Scope): LoopStatement {
		return LoopStatement(this, generator?.concretize(linter, scope), body.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Loop${if(generator == null) "" else " [ $generator ]"} { $body }"
	}
}