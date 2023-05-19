package components.syntax_parser.syntax_tree.access

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import source_structure.Position
import util.indent
import util.toLines
import util.toSemanticTypeModels
import util.toSemanticValueModels
import components.semantic_analysis.semantic_model.operations.IndexAccess as SemanticIndexAccessModel

class IndexAccess(private val target: ValueElement, private val typeParameters: List<TypeElement>?,
				  private val indices: List<ValueElement>, end: Position): ValueElement(target.start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticIndexAccessModel {
		return SemanticIndexAccessModel(this, scope, target.toSemanticModel(scope),
			typeParameters.toSemanticTypeModels(scope), indices.toSemanticValueModels(scope))
	}

	override fun toString(): String {
		var stringRepresentation = "Index [ "
		stringRepresentation += target
		stringRepresentation += " ] {"
		if(typeParameters != null) {
			stringRepresentation += typeParameters.toLines().indent()
			stringRepresentation += ";"
		}
		stringRepresentation += indices.toLines().indent()
		stringRepresentation += "\n}"
		return stringRepresentation
	}
}
