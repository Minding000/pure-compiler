package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.definitions.DeinitializerDefinition as DeinitializerDefinitionSyntaxTree

class DeinitializerDefinition(override val source: DeinitializerDefinitionSyntaxTree, val scope: BlockScope,
							  val body: Unit?, val isNative: Boolean): Unit(source) {

	init {
		if(body != null)
			units.add(body)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}
}
