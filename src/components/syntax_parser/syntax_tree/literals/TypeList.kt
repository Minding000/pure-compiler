package components.syntax_parser.syntax_tree.literals

import components.linting.Linter
import components.linting.semantic_model.scopes.MutableScope
import components.linting.semantic_model.types.Type as SemanticTypeModel
import components.syntax_parser.syntax_tree.general.MetaElement
import components.syntax_parser.syntax_tree.general.TypeElement
import source_structure.Position
import util.concretizeTypes
import util.indent
import util.toLines

class TypeList(private val typeParameters: List<TypeElement>, start: Position, end: Position): MetaElement(start, end) {

	fun concretizeTypes(linter: Linter, scope: MutableScope): List<SemanticTypeModel> {
		return typeParameters.concretizeTypes(linter, scope)
	}

	override fun toString(): String {
		return "TypeList {${typeParameters.toLines().indent()}\n}"
	}
}
