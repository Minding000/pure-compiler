package components.syntax_parser.syntax_tree.operations

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import util.indent
import util.toLines
import util.toSemanticValueModels
import components.semantic_analysis.semantic_model.operations.Assignment as SemanticAssignmentModel

class Assignment(private val targets: List<ValueSyntaxTreeNode>, val source: ValueSyntaxTreeNode): SyntaxTreeNode(targets.first().start, source.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticAssignmentModel {
		return SemanticAssignmentModel(this, scope, targets.toSemanticValueModels(scope), source.toSemanticModel(scope))
	}

	override fun toString(): String {
		return "Assignment {${"${targets.toLines()}\n= $source".indent()}\n}"
	}
}
