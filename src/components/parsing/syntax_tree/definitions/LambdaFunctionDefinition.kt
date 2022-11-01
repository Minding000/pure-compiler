package components.parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.FunctionImplementation as SemanticFunctionImplementationModel
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.values.Function as SemanticFunctionModel
import linting.semantic_model.scopes.BlockScope
import components.parsing.syntax_tree.general.StatementSection
import components.parsing.syntax_tree.general.TypeElement
import components.parsing.syntax_tree.general.ValueElement
import source_structure.Position

class LambdaFunctionDefinition(start: Position, private val parameterList: ParameterList?,
							   private val body: StatementSection, private val returnType: TypeElement?):
	ValueElement(start, body.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticFunctionModel {
		val functionScope = BlockScope(scope)
		val parameters = parameterList?.concretizeParameters(linter, functionScope) ?: listOf()
		val returnType = returnType?.concretize(linter, scope)
		val implementation = SemanticFunctionImplementationModel(this, functionScope, listOf(), parameters,
			body.concretize(linter, scope), returnType)
		return SemanticFunctionModel(this, implementation)
	}

	override fun toString(): String {
		return "LambdaFunctionDefinition [ ${parameterList ?: ""}${if(returnType == null) "" else ": $returnType"} ] { $body }"
	}
}
