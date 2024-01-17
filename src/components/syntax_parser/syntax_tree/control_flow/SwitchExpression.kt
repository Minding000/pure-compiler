package components.syntax_parser.syntax_tree.control_flow

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines
import java.util.*
import components.semantic_model.control_flow.Case as SemanticCaseModel
import components.semantic_model.control_flow.SwitchExpression as SemanticSwitchExpressionModel

class SwitchExpression(private val subject: ValueSyntaxTreeNode, private val cases: LinkedList<Case>,
					   private val elseBranch: StatementSection?, private val isPartOfExpression: Boolean, start: Position, end: Position):
	ValueSyntaxTreeNode(start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticSwitchExpressionModel {
		val cases = LinkedList<SemanticCaseModel>()
		for(case in this.cases)
			cases.add(case.toSemanticModel(scope))
		return SemanticSwitchExpressionModel(this, scope, subject.toSemanticModel(scope), cases, elseBranch?.toSemanticModel(scope),
			isPartOfExpression)
	}

	override fun toString(): String {
		return "Switch [ $subject ] {${cases.toLines().indent()}\n}${if(elseBranch == null) "" else " Else {${"\n$elseBranch".indent()}\n}"}"
	}
}
