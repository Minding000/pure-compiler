package components.parsing.syntax_tree.operations

import components.linting.Linter
import components.linting.semantic_model.operations.NullCheck as SemanticNullCheckModel
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.ValueElement
import components.parsing.syntax_tree.literals.Identifier

class NullCheck(val identifier: Identifier): ValueElement(identifier.start, identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticNullCheckModel {
		return SemanticNullCheckModel(this, identifier.concretize(linter, scope))
	}

	override fun toString(): String {
		return "NullCheck { $identifier }"
	}
}
