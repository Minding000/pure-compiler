package components.linting.semantic_model.definitions

import components.linting.Linter
import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.scopes.BlockScope
import components.linting.semantic_model.scopes.Scope
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
