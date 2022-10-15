package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.values.Instance as SemanticInstanceModel
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.ValueElement
import parsing.syntax_tree.literals.Identifier
import source_structure.Position
import util.concretizeValues
import util.indent
import util.toLines

class Instance(val identifier: Identifier, val parameters: List<ValueElement>, end: Position):
	Element(identifier.start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticInstanceModel {
		val instance = SemanticInstanceModel(this, identifier.concretize(linter, scope),
			parameters.concretizeValues(linter, scope))
		scope.declareValue(linter, instance)
		return instance
	}

	override fun toString(): String {
		return "Instance [ $identifier ] {${parameters.toLines().indent()}\n}"
	}
}
