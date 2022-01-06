package parsing.ast.definitions

import code.Main
import parsing.ast.Element
import source_structure.Position
import java.lang.StringBuilder

class ParameterList(start: Position, end: Position, val parameters: List<Parameter>): Element(start, end) {

	override fun toString(): String {
		val parameterString = StringBuilder()
		for(parameter in parameters)
			parameterString.append("\n").append(parameter.toString())
		return "ParameterList {${Main.indentText(parameterString.toString())}\n}"
	}
}