package components.semantic_model.declarations

import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.scopes.BlockScope
import components.semantic_model.types.Type
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.definitions.GeneratorDefinition as GeneratorDefinitionSyntaxTree

class GeneratorDefinition(override val source: GeneratorDefinitionSyntaxTree, override val scope: BlockScope, name: String,
						  val parameters: List<Parameter>, val keyReturnType: Type?, val valueReturnType: Type,
						  val body: ErrorHandlingContext): ValueDeclaration(source, scope, name) {

	init {
		addSemanticModels(keyReturnType, valueReturnType, body)
		addSemanticModels(parameters)
	}

	override fun validate() {
		super.validate()
		scope.validate()
	}
}
