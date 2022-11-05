package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.Unit
import components.syntax_parser.syntax_tree.definitions.DeinitializerDefinition as DeinitializerDefinitionSyntaxTree

class DeinitializerDefinition(override val source: DeinitializerDefinitionSyntaxTree, val body: ErrorHandlingContext?,
							  val isNative: Boolean): Unit(source) {

	init {
		addUnits(body)
	}
}
