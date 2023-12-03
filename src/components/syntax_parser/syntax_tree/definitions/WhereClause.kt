package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines
import components.semantic_model.declarations.WhereClauseCondition as WhereClauseConditionSemanticModel

class WhereClause(private val conditions: List<WhereClauseCondition>, start: Position): MetaSyntaxTreeNode(start, conditions.last().end) {

	fun toWhereClauseConditionSemanticModels(scope: MutableScope): List<WhereClauseConditionSemanticModel> {
		return conditions.map { condition -> condition.toSemanticModel(scope) }
	}

	override fun toString(): String {
		return "WhereClause {${conditions.toLines().indent()}\n}"
	}
}
