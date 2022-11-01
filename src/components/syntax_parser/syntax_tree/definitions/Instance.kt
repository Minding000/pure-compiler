package components.syntax_parser.syntax_tree.definitions

import components.linting.Linter
import components.linting.semantic_model.scopes.MutableScope
import components.linting.semantic_model.values.Instance as SemanticInstanceModel
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.Identifier
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
