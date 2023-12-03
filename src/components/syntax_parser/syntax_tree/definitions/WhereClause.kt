package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines
import components.semantic_model.declarations.WhereClause as WhereClauseSemanticModel

class WhereClause(private val conditions: List<WhereClauseCondition>, start: Position): SyntaxTreeNode(start, conditions.last().end) {

	override fun toSemanticModel(scope: MutableScope): WhereClauseSemanticModel {
		return WhereClauseSemanticModel(this, scope, conditions.map { condition -> condition.toSemanticModel(scope) })
	}

	override fun toString(): String {
		return "WhereClause {${conditions.toLines().indent()}\n}"
	}
}
