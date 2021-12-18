package parsing.ast.definitions

import code.Main
import parsing.ast.Element
import source_structure.Position
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import java.lang.StringBuilder

class FunctionDefinition(start: Position, end: Position, val identifier: Identifier,
						 val parameters: List<TypedIdentifier>, val statements: List<Element>,
						 var returnType: Type?): Element(start, end) {

	override fun toString(): String {
		val parameterString = StringBuilder()
		for(parameter in parameters)
			parameterString.append("\n").append(parameter.toString())
		val statementString = StringBuilder()
		for(statement in statements)
			statementString.append("\n").append(statement.toString())
		return "Function [$identifier(${Main.indentText(parameterString.toString())}\n): ${returnType ?: "void"}] {${Main.indentText(statementString.toString())}\n}"
	}
}