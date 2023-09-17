package components.syntax_parser.syntax_tree.general

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.literals.Identifier
import components.semantic_model.general.ReferenceAlias as SemanticReferenceAliasModel

class ReferenceAlias(private val originalName: Identifier, private val localName: Identifier):
	SyntaxTreeNode(originalName.start, localName.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticReferenceAliasModel {
		return SemanticReferenceAliasModel(this, scope, originalName.getValue(), localName.getValue())
	}

	override fun toString(): String {
		return "Alias { $originalName as $localName }"
	}
}
