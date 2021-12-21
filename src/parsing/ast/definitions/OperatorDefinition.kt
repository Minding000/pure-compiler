package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.general.StatementBlock
import source_structure.Position
import parsing.ast.literals.Type

class OperatorDefinition(start: Position, val modifierList: ModifierList?, val operator: Operator,
						 val parameterList: ParameterList?, val body: StatementBlock,
						 var returnType: Type?): Element(start, body.end) {

	override fun toString(): String {
		val string = StringBuilder()
		string.append("OperatorDefinition [")
		if(modifierList != null)
			string.append(modifierList)
				.append(" ")
		string.append(operator)
		if(parameterList != null)
			string.append(" ")
				.append(parameterList)
		string.append(": ")
			.append(returnType ?: "void")
			.append("] { ")
			.append(body)
			.append(" }")
		return string.toString()
	}
}