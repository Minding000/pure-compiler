package parsing.ast.definitions

import code.Main
import source_structure.Position
import java.lang.StringBuilder

class IndexOperator(start: Position, end: Position, val parameters: List<TypedIdentifier>): Operator(start, end) {

	override fun toString(): String {
		val parameterString = StringBuilder()
		for(parameter in parameters)
			parameterString.append("\n").append(parameter.toString())
		return "IndexOperator {${Main.indentText(parameterString.toString())}\n}"
	}
}