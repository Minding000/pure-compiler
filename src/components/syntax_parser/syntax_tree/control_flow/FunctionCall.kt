package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import source_structure.Position
import util.indent
import util.toLines
import util.toSemanticTypeModels
import util.toSemanticValueModels
import components.semantic_analysis.semantic_model.control_flow.FunctionCall as SemanticFunctionCallModel

class FunctionCall(private val functionReference: ValueElement, private val typeParameters: List<TypeElement>?,
				   private val valueParameters: List<ValueElement>, end: Position): ValueElement(functionReference.start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticFunctionCallModel {
		return SemanticFunctionCallModel(this, scope, functionReference.toSemanticModel(scope),
			typeParameters.toSemanticTypeModels(scope), valueParameters.toSemanticValueModels(scope))
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
