package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import source_structure.Position
import components.semantic_analysis.semantic_model.control_flow.Try as SemanticTryModel

class Try(private val expression: ValueSyntaxTreeNode, private val isOptional: Boolean, start: Position): ValueSyntaxTreeNode(start, expression.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticTryModel {
		return SemanticTryModel(this, scope, expression.toSemanticModel(scope), isOptional)
	}

	override fun toString(): String {
		return "Try [ ${if(isOptional) "null" else "uncheck"} ] { $expression }"
	}
}
