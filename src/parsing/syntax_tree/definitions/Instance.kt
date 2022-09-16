package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.values.Instance as SemanticInstanceModel
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.literals.Identifier

class Instance(val identifier: Identifier): Element(identifier.start, identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticInstanceModel {
		val instance = SemanticInstanceModel(this, identifier.concretize(linter, scope))
		scope.declareValue(linter, instance)
		return instance
	}

	override fun toString(): String {
		return "Instance { $identifier }"
	}
}