package parsing.ast.definitions

import code.Main
import parsing.ast.Element
import parsing.ast.general.StatementBlock
import source_structure.Position
import java.lang.StringBuilder

class InitializerDefinition(start: Position, end: Position, val modifierList: ModifierList?, val parameters: List<Element>, val body: StatementBlock?): Element(start, end) {

	override fun toString(): String {
		val parameterString = StringBuilder()
		for(parameter in parameters)
			parameterString.append("\n").append(parameter.toString())
		return "Initializer [${if(modifierList == null) "" else "$modifierList "}${Main.indentText(parameterString.toString())}\n] { ${body ?: ""} }"
	}
}