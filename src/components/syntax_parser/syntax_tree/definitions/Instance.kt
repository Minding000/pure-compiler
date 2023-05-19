package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import util.concretizeValues
import util.indent
import util.toLines
import components.semantic_analysis.semantic_model.values.Instance as SemanticInstanceModel

class Instance(val identifier: Identifier, val parameters: List<ValueElement>, end: Position): Element(identifier.start, end) {

	override fun concretize(scope: MutableScope): SemanticInstanceModel {
		return SemanticInstanceModel(this, scope, identifier.concretize(scope), parameters.concretizeValues(scope))
	}

	override fun toString(): String {
		return "Instance [ $identifier ] {${parameters.toLines().indent()}\n}"
	}
}
