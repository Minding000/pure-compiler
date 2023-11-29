package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.MutableScope
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.ObjectType
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import components.semantic_model.declarations.WhereClause as WhereClauseSemanticModel

class WhereClause(val subject: Identifier, val override: TypeSyntaxTreeNode, start: Position): SyntaxTreeNode(start, override.end) {

	override fun toSemanticModel(scope: MutableScope): WhereClauseSemanticModel {
		val override = override.toSemanticModel(scope)
		val typeScope = TypeScope(scope)
		typeScope.superScope = override.interfaceScope
		val subject = ObjectType(this, (scope as? BlockScope)?.parentScope ?: scope, subject.getValue())
		return WhereClauseSemanticModel(this, typeScope, subject, override)
	}

	override fun toString(): String {
		return "WhereClause {\n\t$subject is $override\n}"
	}
}
