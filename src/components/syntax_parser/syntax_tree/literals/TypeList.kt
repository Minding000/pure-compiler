package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.MetaElement
import components.syntax_parser.syntax_tree.general.TypeElement
import source_structure.Position
import util.indent
import util.toLines
import util.toSemanticTypeModels
import components.semantic_analysis.semantic_model.types.Type as SemanticTypeModel

class TypeList(private val typeParameters: List<TypeElement>, start: Position, end: Position): MetaElement(start, end) {

	fun toSemanticModels(scope: MutableScope): List<SemanticTypeModel> {
		return typeParameters.toSemanticTypeModels(scope)
	}

	override fun toString(): String {
		return "TypeList {${typeParameters.toLines().indent()}\n}"
	}
}
