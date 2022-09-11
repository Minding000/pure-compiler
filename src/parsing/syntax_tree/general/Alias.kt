package parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.general.ReferenceAlias
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.literals.Identifier

class Alias(private val originalName: Identifier, private val aliasName: Identifier): Element(originalName.start, aliasName.end) {

	override fun concretize(linter: Linter, scope: MutableScope): ReferenceAlias {
		return ReferenceAlias(this, originalName.getValue(), aliasName.getValue())
	}

	override fun toString(): String {
		return "Alias { $originalName as $aliasName }"
	}
}