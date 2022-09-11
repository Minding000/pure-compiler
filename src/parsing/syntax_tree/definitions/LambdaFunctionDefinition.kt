package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.FunctionImplementation
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.values.Function
import linting.semantic_model.scopes.BlockScope
import parsing.syntax_tree.general.StatementSection
import parsing.syntax_tree.general.TypeElement
import parsing.syntax_tree.general.ValueElement
import source_structure.Position

class LambdaFunctionDefinition(start: Position, private val parameterList: ParameterList?,
							   private val body: StatementSection, private val returnType: TypeElement?):
	ValueElement(start, body.end) {

	override fun concretize(linter: Linter, scope: MutableScope): Function {
		val functionScope = BlockScope(scope)
		val parameters = parameterList?.concretizeParameters(linter, functionScope) ?: listOf()
		val returnType = returnType?.concretize(linter, scope)
		val implementation = FunctionImplementation(this, functionScope, listOf(), parameters,
			body.concretize(linter, scope), returnType)
		return Function(this, implementation)
	}

	override fun toString(): String {
		return "LambdaFunctionDefinition [ ${parameterList ?: ""}${if(returnType == null) "" else ": $returnType"} ] { $body }"
	}
}