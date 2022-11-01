package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.ReferenceAlias as SemanticReferenceAliasModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.literals.Identifier

class ReferenceAlias(private val originalName: Identifier, private val aliasName: Identifier):
	Element(originalName.start, aliasName.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticReferenceAliasModel {
		return SemanticReferenceAliasModel(this, originalName.getValue(), aliasName.getValue())
	}

	override fun toString(): String {
		return "Alias { $originalName as $aliasName }"
	}
}
