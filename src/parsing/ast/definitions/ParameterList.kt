package parsing.ast.definitions

import linter.Linter
import linter.scopes.MutableScope
import linter.elements.definitions.Parameter as LinterParameter
import parsing.ast.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines
import java.util.*

class ParameterList(start: Position, end: Position, val parameters: List<Parameter>): MetaElement(start, end) {

	fun concretizeParameters(linter: Linter, scope: MutableScope): List<LinterParameter> {
		val parameters = LinkedList<LinterParameter>()
		for(parameter in this.parameters)
			parameters.add(parameter.concretize(linter, scope))
		return parameters
	}

	override fun toString(): String {
		return "ParameterList {${parameters.toLines().indent()}\n}"
	}
}