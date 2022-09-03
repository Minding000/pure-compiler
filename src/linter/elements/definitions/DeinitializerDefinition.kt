package linter.elements.definitions

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.BlockScope
import linter.scopes.Scope
import parsing.ast.definitions.DeinitializerDefinition

class DeinitializerDefinition(val source: DeinitializerDefinition, val scope: BlockScope, val body: Unit?,
							  val isNative: Boolean): Unit() {

	init {
		if(body != null)
			units.add(body)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}
}