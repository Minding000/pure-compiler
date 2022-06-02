package linter.elements.control_flow

import linter.elements.general.Unit
import linter.elements.values.Value
import parsing.ast.control_flow.FunctionCall

class FunctionCall(val source: FunctionCall, val context: Unit?, val name: String, val parameters: List<Unit>): Value() {

	init {
		if(context != null)
			units.add(context)
		units.addAll(parameters)
	}
}