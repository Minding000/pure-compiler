package components.semantic_model.declarations

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.definitions.WhereClause as WhereClauseSyntaxTree

//TODO consider making the where clause a meta-element, because it just wraps a list of conditions.
class WhereClause(source: WhereClauseSyntaxTree, scope: Scope, val conditions: List<WhereClauseCondition>): SemanticModel(source, scope) {

	init {
		addSemanticModels(conditions)
	}
}
