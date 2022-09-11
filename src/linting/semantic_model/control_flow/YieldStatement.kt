package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import parsing.syntax_tree.control_flow.YieldStatement

class YieldStatement(override val source: YieldStatement, val key: Unit?, val value: Unit): Value(source) {

	init {
		if(key != null)
			units.add(key)
		units.add(value)
	}
}