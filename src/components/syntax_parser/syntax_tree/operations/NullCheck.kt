package components.syntax_parser.syntax_tree.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.Identifier
import components.semantic_analysis.semantic_model.operations.NullCheck as SemanticNullCheckModel

class NullCheck(val identifier: Identifier): ValueElement(identifier.start, identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticNullCheckModel {
		return SemanticNullCheckModel(this, scope, identifier.concretize(linter, scope))
	}

	override fun toString(): String {
		return "NullCheck { $identifier }"
	}
}
