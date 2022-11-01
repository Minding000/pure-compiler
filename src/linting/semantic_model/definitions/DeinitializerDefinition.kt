package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.Scope
import components.parsing.syntax_tree.definitions.DeinitializerDefinition as DeinitializerDefinitionSyntaxTree

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
