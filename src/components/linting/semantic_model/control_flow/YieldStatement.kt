package components.linting.semantic_model.control_flow

import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.YieldStatement as YieldStatementSyntaxTree

class YieldStatement(override val source: YieldStatementSyntaxTree, val key: Unit?, val value: Value): Value(source) {

	init {
		if(key != null)
			units.add(key)
		units.add(value)
	}
}
