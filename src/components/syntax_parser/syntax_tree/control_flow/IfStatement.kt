package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import source_structure.Position
import util.indent
import components.semantic_analysis.semantic_model.control_flow.IfStatement as SemanticIfStatementModel

class IfStatement(private val condition: ValueSyntaxTreeNode, private val positiveBranch: SyntaxTreeNode,
				  private val negativeBranch: SyntaxTreeNode?, start: Position, end: Position): SyntaxTreeNode(start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticIfStatementModel {
		return SemanticIfStatementModel(this, scope, condition.toSemanticModel(scope),
			positiveBranch.toSemanticModel(scope), negativeBranch?.toSemanticModel(scope))
	}

	override fun toString(): String {
		var stringRepresentation = "If [ $condition ] {${"\n$positiveBranch".indent()}\n}"
		if(negativeBranch != null)
			stringRepresentation += " Else {${"\n$negativeBranch".indent()}\n}"
		return stringRepresentation
	}
}
