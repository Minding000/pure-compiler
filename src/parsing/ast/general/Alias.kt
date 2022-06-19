package parsing.ast.general

import linter.Linter
import linter.elements.general.ReferenceAlias
import linter.scopes.MutableScope
import parsing.ast.literals.Identifier

class Alias(private val originalName: Identifier, private val aliasName: Identifier): Element(originalName.start, aliasName.end) {

	override fun concretize(linter: Linter, scope: MutableScope): ReferenceAlias {
		return ReferenceAlias(this, originalName.getValue(), aliasName.getValue())
	}

	override fun toString(): String {
		return "Alias { $originalName as $aliasName }"
	}
}