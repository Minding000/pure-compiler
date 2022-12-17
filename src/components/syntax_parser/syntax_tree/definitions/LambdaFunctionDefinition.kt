package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation as SemanticFunctionImplementationModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.values.Function as SemanticFunctionModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import source_structure.Position

class LambdaFunctionDefinition(start: Position, private val parameterList: ParameterList?,
							   private val body: StatementSection, private val returnType: TypeElement?):
	ValueElement(start, body.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticFunctionModel {
		val functionScope = BlockScope(scope)
		val parameters = parameterList?.concretizeParameters(linter, functionScope) ?: listOf()
		val returnType = returnType?.concretize(linter, functionScope)
		val implementation = SemanticFunctionImplementationModel(this, null, functionScope, listOf(),
			parameters, body.concretize(linter, functionScope), returnType)
		return SemanticFunctionModel(this, implementation)
	}

	override fun toString(): String {
		return "LambdaFunctionDefinition [ ${parameterList ?: ""}${if(returnType == null) "" else ": $returnType"} ] { $body }"
	}
}
