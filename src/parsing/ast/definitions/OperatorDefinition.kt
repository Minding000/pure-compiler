package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.OperatorDefinition
import linter.scopes.BlockScope
import linter.scopes.Scope
import linter.elements.definitions.Parameter
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
		val operatorScope = BlockScope(scope)
		//TODO include modifiers and operator type
		val parameters = LinkedList<Parameter>()
		if(parameterList != null) {
			for(parameter in parameterList.parameters)
				parameters.add(parameter.concretize(linter, operatorScope))
		}
		val name = if(operator is IndexOperator) {
			for(parameter in operator.parameters)
				parameters.add(parameter.concretize(linter, operatorScope))
			operator.getSignature()
		} else {
			operator.getValue()
		}
		val operatorDefinition = OperatorDefinition(this, name, operatorScope, parameters,
			body?.concretize(linter, operatorScope), returnType?.concretize(linter, operatorScope))
		scope.declareOperator(linter, operatorDefinition)
		return operatorDefinition
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