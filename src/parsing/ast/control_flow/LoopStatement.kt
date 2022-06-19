package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.LoopStatement
import linter.scopes.BlockScope
import linter.scopes.MutableScope
import parsing.ast.general.Element
import parsing.ast.general.StatementSection
import source_structure.Position

class LoopStatement(start: Position, private val generator: Element?, private val body: StatementSection): Element(start, body.end) {

	override fun concretize(linter: Linter, scope: MutableScope): LoopStatement {
		val loopScope = BlockScope(scope)
		return LoopStatement(this, loopScope, generator?.concretize(linter, loopScope),
			body.concretize(linter, loopScope))
	}

	override fun toString(): String {
		return "Loop${if(generator == null) "" else " [ $generator ]"} { $body }"
	}
}