package parsing.ast.control_flow

import code.Main
import source_structure.Position
import parsing.ast.Element
import parsing.ast.access.ReferenceChain
import parsing.ast.literals.TypeList
import java.lang.StringBuilder

class FunctionCall(val typeList: TypeList?, val functionReference: Element, val parameters: List<Element>, start: Position, end: Position): Element(start, end) {

	override fun toString(): String {
		val string = StringBuilder()
		for(parameter in parameters)
			string.append("\n").append(parameter.toString())
		return "FunctionCall [${if(typeList != null) "$typeList " else ""}$functionReference] {${Main.indentText(string.toString())}\n}"
	}
}