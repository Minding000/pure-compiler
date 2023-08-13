package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import source_structure.Position
import components.semantic_analysis.semantic_model.declarations.FunctionImplementation as SemanticFunctionImplementationModel
import components.semantic_analysis.semantic_model.values.Function as SemanticFunctionModel

class LambdaFunctionDefinition(start: Position, private val parameterList: ParameterList?, private val body: StatementSection,
							   private val returnType: TypeSyntaxTreeNode?): ValueSyntaxTreeNode(start, body.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticFunctionModel {
		val functionScope = BlockScope(scope)
		val parameters = parameterList?.getSemanticParameterModels(functionScope) ?: emptyList()
		val returnType = returnType?.toSemanticModel(functionScope)
		val implementation = SemanticFunctionImplementationModel(this, functionScope, emptyList(), parameters,
			body.toSemanticModel(functionScope), returnType)
		val function = SemanticFunctionModel(this, scope)
		function.addImplementation(implementation)
		return function
	}

	override fun toString(): String {
		return "LambdaFunctionDefinition [ ${parameterList ?: ""}${if(returnType == null) "" else ": $returnType"} ] { $body }"
	}
}
