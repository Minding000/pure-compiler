package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import parsing.syntax_tree.control_flow.IfStatement

class IfStatement(val source: IfStatement, val trueBranch: Unit, val falseBranch: Unit?): Unit() {

	init {
		units.add(trueBranch)
		if(falseBranch != null)
			units.add(falseBranch)
	}
}