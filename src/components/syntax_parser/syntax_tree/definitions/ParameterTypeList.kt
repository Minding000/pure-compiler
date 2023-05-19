package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.MetaElement
import components.syntax_parser.syntax_tree.general.TypeElement
import source_structure.Position
import util.indent
import util.toLines
import util.toSemanticTypeModels

class ParameterTypeList(start: Position, val parameters: List<TypeElement>, end: Position): MetaElement(start, end) {

	fun toSemanticModels(scope: MutableScope): List<Type> {
		return parameters.toSemanticTypeModels(scope)
	}

	override fun toString(): String {
		return "ParameterTypeList {${parameters.toLines().indent()}\n}"
	}
}
