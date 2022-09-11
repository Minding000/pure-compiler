package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.MetaElement
import parsing.syntax_tree.general.TypeElement
import source_structure.Position
import util.indent
import util.toLines
import java.util.*

class ParameterTypeList(start: Position, val parameters: List<TypeElement>, end: Position): MetaElement(start, end) {

	fun concretizeTypes(linter: Linter, scope: MutableScope): List<Type> {
		val parameters = LinkedList<Type>()
		for(parameter in this.parameters)
			parameters.add(parameter.concretize(linter, scope))
		return parameters
	}

	override fun toString(): String {
		return "ParameterTypeList {${parameters.toLines().indent()}\n}"
	}
}