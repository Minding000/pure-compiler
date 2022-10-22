package parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.types.Type
import parsing.syntax_tree.general.MetaElement
import parsing.syntax_tree.general.TypeElement
import source_structure.Position
import util.concretizeTypes
import util.indent
import util.toLines

class TypeList(private val typeParameters: List<TypeElement>, start: Position, end: Position): MetaElement(start, end) {

	fun concretizeTypes(linter: Linter, scope: MutableScope): List<Type> {
		return typeParameters.concretizeTypes(linter, scope)
	}

	override fun toString(): String {
		return "TypeList {${typeParameters.toLines().indent()}\n}"
	}
}
