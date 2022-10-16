package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.IndexOperatorDefinition
import linting.semantic_model.definitions.OperatorDefinition
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.definitions.Parameter
import parsing.syntax_tree.definitions.sections.OperatorSection
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.StatementSection
import parsing.syntax_tree.general.TypeElement
import java.util.*

class OperatorDefinition(private val operator: Operator, private val genericsList: GenericsList?,
						 private val parameterList: ParameterList?, private val body: StatementSection?,
						 private var returnType: TypeElement?):
	Element(operator.start, body?.end ?: returnType?.end ?: parameterList?.end ?: operator.end) {
	lateinit var parent: OperatorSection

	override fun concretize(linter: Linter, scope: MutableScope): OperatorDefinition {
		val operatorScope = BlockScope(scope)
		//TODO include modifiers
		val genericParameters = genericsList?.concretizeGenerics(linter, scope) ?: listOf()
		val parameters = LinkedList<Parameter>()
		if(parameterList != null) {
			for(parameter in parameterList.parameters)
				parameters.add(parameter.concretize(linter, operatorScope))
		}
		val operatorDefinition = if(operator is IndexOperator) {
			val indices = LinkedList<Parameter>()
			for(index in operator.indices)
				indices.add(index.concretize(linter, operatorScope))
			IndexOperatorDefinition(this, operatorScope, genericParameters, indices, parameters,
				body?.concretize(linter, operatorScope), returnType?.concretize(linter, operatorScope))
		} else {
			OperatorDefinition(this, operator.getValue(), operatorScope, genericParameters, parameters,
				body?.concretize(linter, operatorScope), returnType?.concretize(linter, operatorScope))
		}
		scope.declareOperator(linter, operatorDefinition)
		return operatorDefinition
	}

	override fun toString(): String {
		val string = StringBuilder()
		string.append("OperatorDefinition [ ")
		if(genericsList != null)
			string.append(genericsList)
				.append(" ")
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
