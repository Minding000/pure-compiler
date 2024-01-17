package components.syntax_parser.syntax_tree.control_flow

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import util.indent
import components.semantic_model.control_flow.Case as SemanticCaseModel

class Case(private val condition: ValueSyntaxTreeNode, private val result: StatementSection): SyntaxTreeNode(condition.start, result.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticCaseModel {
		return SemanticCaseModel(this, scope, condition.toSemanticModel(scope), result.toSemanticModel(scope))
	}

	override fun toString(): String {
		return "Case [ $condition ] {${"\n$result".indent()}\n}"
	}
}
