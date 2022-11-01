package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.control_flow.FunctionCall as SemanticFunctionCallModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeElement
import source_structure.Position
import components.syntax_parser.syntax_tree.general.ValueElement
import util.concretizeTypes
import util.concretizeValues
import util.indent
import util.toLines

class FunctionCall(private val functionReference: ValueElement, private val typeParameters: List<TypeElement>?,
				   private val valueParameters: List<ValueElement>, end: Position):
	ValueElement(functionReference.start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticFunctionCallModel {
		return SemanticFunctionCallModel(this, functionReference.concretize(linter, scope),
			typeParameters.concretizeTypes(linter, scope), valueParameters.concretizeValues(linter, scope))
	}

	override fun toString(): String {
		var stringRepresentation = "FunctionCall [ "
		stringRepresentation += functionReference
		stringRepresentation += " ] {"
		if(typeParameters != null) {
			stringRepresentation += typeParameters.toLines().indent()
			stringRepresentation += ";"
		}
		stringRepresentation += valueParameters.toLines().indent()
		stringRepresentation += "\n}"
		return stringRepresentation
	}
}
