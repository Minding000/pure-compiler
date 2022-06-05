package linter.elements.control_flow

import linter.elements.general.Unit
import linter.elements.values.Value
import parsing.ast.control_flow.YieldStatement

class YieldStatement(val source: YieldStatement, val key: Unit?, val value: Unit): Value() {

	init {
		if(key != null)
			units.add(key)
		units.add(value)
	}
}