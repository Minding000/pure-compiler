package components.syntax_parser.syntax_tree.operations

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.Operator
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import util.indent
import components.semantic_analysis.semantic_model.operations.BinaryModification as SemanticBinaryModificationModel

class BinaryModification(val target: ValueElement, val modifier: ValueElement, val operator: Operator):
	Element(target.start, modifier.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticBinaryModificationModel {
		return SemanticBinaryModificationModel(this, scope, target.toSemanticModel(scope), modifier.toSemanticModel(scope),
			operator.getKind())
	}

	override fun toString(): String {
		return "BinaryModification {${"\n$target $operator $modifier".indent()}\n}"
	}
}
