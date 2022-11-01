package components.parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.general.ReferenceAlias as SemanticReferenceAliasModel
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.literals.Identifier

class ReferenceAlias(private val originalName: Identifier, private val aliasName: Identifier):
	Element(originalName.start, aliasName.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticReferenceAliasModel {
		return SemanticReferenceAliasModel(this, originalName.getValue(), aliasName.getValue())
	}

	override fun toString(): String {
		return "Alias { $originalName as $aliasName }"
	}
}
