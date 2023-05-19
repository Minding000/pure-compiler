package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import source_structure.Position
import components.semantic_analysis.semantic_model.control_flow.RaiseStatement as SemanticRaiseStatementModel

class RaiseStatement(private val value: ValueSyntaxTreeNode, start: Position): SyntaxTreeNode(start, value.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticRaiseStatementModel {
		return SemanticRaiseStatementModel(this, scope, value.toSemanticModel(scope))
	}

	override fun toString(): String {
		return "Raise { $value }"
	}
}
