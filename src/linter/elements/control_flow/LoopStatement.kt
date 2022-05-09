package linter.elements.control_flow

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.BlockScope
import linter.scopes.Scope
import parsing.ast.control_flow.LoopStatement

class LoopStatement(val source: LoopStatement, val scope: BlockScope, val generator: Unit?, val body: Unit): Unit() {

	init {
		if(generator != null)
			units.add(generator)
		units.add(body)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, this.scope)
	}
}