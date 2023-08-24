package components.semantic_analysis.semantic_model.declarations

import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.definitions.GeneratorDefinition as GeneratorDefinitionSyntaxTree

class GeneratorDefinition(override val source: GeneratorDefinitionSyntaxTree, override val scope: BlockScope, name: String,
						  val parameters: List<Parameter>, val keyReturnType: Type?, val valueReturnType: Type,
						  val body: ErrorHandlingContext): ValueDeclaration(source, scope, name) {

	init {
		addSemanticModels(keyReturnType, valueReturnType, body)
		addSemanticModels(parameters)
	}
}
