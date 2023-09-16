package components.semantic_model.declarations

import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.definitions.DeinitializerDefinition as DeinitializerDefinitionSyntaxTree

class DeinitializerDefinition(override val source: DeinitializerDefinitionSyntaxTree, scope: Scope, val body: ErrorHandlingContext?,
							  val isNative: Boolean): SemanticModel(source, scope) {

	init {
		addSemanticModels(body)
	}
}
