package components.syntax_parser.syntax_tree.control_flow

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines
import java.util.*
import components.semantic_model.control_flow.Case as SemanticCaseModel
import components.semantic_model.control_flow.SwitchStatement as SemanticSwitchStatementModel

class SwitchStatement(private val subject: ValueSyntaxTreeNode, private val cases: LinkedList<Case>, private val elseBranch: SyntaxTreeNode?,
					  start: Position, end: Position): SyntaxTreeNode(start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticSwitchStatementModel {
		val cases = LinkedList<SemanticCaseModel>()
		for(case in this.cases)
			cases.add(case.toSemanticModel(scope))
		return SemanticSwitchStatementModel(this, scope, subject.toSemanticModel(scope), cases,
			elseBranch?.toSemanticModel(scope))
	}

	override fun toString(): String {
		return "Switch [ $subject ] {${cases.toLines().indent()}\n}${if(elseBranch == null) "" else " Else {${"\n$elseBranch".indent()}\n}"}"
	}
}
