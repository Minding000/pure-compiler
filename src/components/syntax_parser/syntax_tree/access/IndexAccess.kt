package components.syntax_parser.syntax_tree.access

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.operations.IndexAccess as SemanticIndexAccessModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import source_structure.Position
import util.*

class IndexAccess(private val target: ValueElement, private val typeParameters: List<TypeElement>?,
				  private val indices: List<ValueElement>, end: Position): ValueElement(target.start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticIndexAccessModel {
		return SemanticIndexAccessModel(this, target.concretize(linter, scope),
			typeParameters.concretizeTypes(linter, scope), indices.concretizeValues(linter, scope))
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
