package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.definitions.DeinitializerDefinition as DeinitializerDefinitionSyntaxTree

class DeinitializerDefinition(override val source: DeinitializerDefinitionSyntaxTree, scope: Scope, val body: ErrorHandlingContext?,
							  val isNative: Boolean): Unit(source, scope) {

	init {
		addUnits(body)
	}
}
