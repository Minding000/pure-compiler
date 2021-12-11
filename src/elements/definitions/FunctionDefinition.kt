package elements.definitions

import code.Main
import elements.identifier.VariableIdentifier
import elements.Element
import elements.VoidElement
import source_structure.Position
import types.Type
import java.lang.StringBuilder

class FunctionDefinition(start: Position, end: Position, val identifier: VariableIdentifier,
						 val parameters: List<VariableIdentifier>, val statements: List<Element>,
						 var returnType: Type?): VoidElement(start, end) {

	override fun toString(): String {
		val parameterString = StringBuilder()
		for(parameter in parameters)
			parameterString.append("\n").append(parameter.toString())
		val statementString = StringBuilder()
		for(statement in statements)
			statementString.append("\n").append(statement.toString())
		return "Function [$identifier(${Main.indentText(parameterString.toString())}\n)] {${Main.indentText(statementString.toString())}\n}"
	}
}