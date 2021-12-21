package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.general.StatementBlock
import source_structure.Position
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import java.lang.StringBuilder

class FunctionDefinition(start: Position, val modifierList: ModifierList?, val identifier: Identifier,
						 val parameterList: ParameterList, val body: StatementBlock,
						 var returnType: Type?): Element(start, body.end) {

	override fun toString(): String {
		val string = StringBuilder()
		string.append("Function [")
		if(modifierList != null)
			string.append(modifierList)
				.append(" ")
		string.append(identifier)
			.append(" ")
			.append(parameterList)
			.append(": ")
			.append(returnType ?: "void")
			.append("] { ")
			.append(body)
			.append(" }")
		return string.toString()
	}
}