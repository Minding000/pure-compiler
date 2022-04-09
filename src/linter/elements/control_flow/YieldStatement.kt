package linter.elements.control_flow

import linter.elements.general.Unit
import parsing.ast.control_flow.YieldStatement

class YieldStatement(val source: YieldStatement, val key: Unit?, val value: Unit): Unit() {

	init {
		if(key != null)
			units.add(key)
		units.add(value)
	}
}