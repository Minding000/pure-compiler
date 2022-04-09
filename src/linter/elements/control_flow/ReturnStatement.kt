package linter.elements.control_flow

import linter.elements.general.Unit
import parsing.ast.control_flow.ReturnStatement

class ReturnStatement(val source: ReturnStatement, val value: Unit?): Unit() {

	init {
		if(value != null)
			units.add(value)
	}
}