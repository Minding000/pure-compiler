package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.FunctionImplementation
import linter.scopes.MutableScope
import linter.elements.values.Function
import linter.scopes.BlockScope
import parsing.ast.general.StatementSection
import parsing.ast.general.TypeElement
import parsing.ast.general.ValueElement
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