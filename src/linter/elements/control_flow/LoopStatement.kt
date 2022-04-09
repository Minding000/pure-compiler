package linter.elements.control_flow

import linter.elements.general.Unit
import parsing.ast.control_flow.LoopStatement

class LoopStatement(val source: LoopStatement, val generator: Unit?, val body: Unit): Unit() {

	init {
		if(generator != null)
			units.add(generator)
		units.add(body)
	}
}