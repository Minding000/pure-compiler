package components.syntax_parser.syntax_tree.control_flow

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import source_structure.Position
import components.semantic_model.control_flow.YieldStatement as SemanticYieldStatementModel

class YieldStatement(start: Position, private val key: ValueSyntaxTreeNode?, private val value: ValueSyntaxTreeNode):
	ValueSyntaxTreeNode(start, value.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticYieldStatementModel {
		return SemanticYieldStatementModel(this, scope, key?.toSemanticModel(scope), value.toSemanticModel(scope))
	}

	override fun toString(): String {
		return "Yield { ${if(key == null) "" else "$key "}$value }"
	}
}
