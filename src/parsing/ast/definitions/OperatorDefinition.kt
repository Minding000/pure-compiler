package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.OperatorDefinition
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.definitions.sections.OperatorSection
import parsing.ast.general.Element
import parsing.ast.general.StatementSection
import parsing.ast.literals.Type
import java.util.*

class OperatorDefinition(private val operator: Operator, private val parameterList: ParameterList?,
						 private val body: StatementSection?, private var returnType: Type?):
	Element(operator.start, body?.end ?: returnType?.end ?: parameterList?.end ?: operator.end) {
	lateinit var parent: OperatorSection

	override fun concretize(linter: Linter, scope: Scope): OperatorDefinition {
		//TODO include modifiers and operator type
		val parameters = LinkedList<Unit>()
		if(parameterList != null) {
			for(parameter in parameterList.parameters)
				parameters.add(parameter.concretize(linter, scope))
		}
		return OperatorDefinition(this, parameters, body?.concretize(linter, scope), returnType?.concretize(linter, scope))
	}

	override fun toString(): String {
		val string = StringBuilder()
		string.append("OperatorDefinition [ ")
		string.append(operator)
		if(parameterList != null)
			string.append(" ")
				.append(parameterList)
		string.append(": ")
			.append(returnType ?: "void")
			.append(" ] { ")
			.append(body)
			.append(" }")
		return string.toString()
	}
}