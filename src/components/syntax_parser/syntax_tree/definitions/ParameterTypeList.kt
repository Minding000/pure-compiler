package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.MetaElement
import components.syntax_parser.syntax_tree.general.TypeElement
import source_structure.Position
import util.concretizeTypes
import util.indent
import util.toLines

class ParameterTypeList(start: Position, val parameters: List<TypeElement>, end: Position): MetaElement(start, end) {

	fun concretizeTypes(linter: Linter, scope: MutableScope): List<Type> {
		return parameters.concretizeTypes(linter, scope)
	}

	override fun toString(): String {
		return "ParameterTypeList {${parameters.toLines().indent()}\n}"
	}
}
