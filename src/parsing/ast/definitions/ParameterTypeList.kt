package parsing.ast.definitions

import linter.Linter
import linter.elements.literals.Type
import linter.scopes.MutableScope
import parsing.ast.general.MetaElement
import parsing.ast.general.TypeElement
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