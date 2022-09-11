package parsing.syntax_tree.operations

import linting.Linter
import linting.semantic_model.operations.NullCheck
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import parsing.syntax_tree.literals.Identifier

class NullCheck(val identifier: Identifier): ValueElement(identifier.start, identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): NullCheck {
		return NullCheck(this, identifier.concretize(linter, scope))
	}

	override fun toString(): String {
		return "NullCheck { $identifier }"
	}
}