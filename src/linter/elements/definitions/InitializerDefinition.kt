package linter.elements.definitions

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.BlockScope
import linter.scopes.Scope
import parsing.ast.definitions.InitializerDefinition

class InitializerDefinition(val source: InitializerDefinition, val scope: BlockScope, val parameters: List<Parameter>,
							val body: Unit?, val isNative: Boolean): Unit() {

	init {
		units.addAll(parameters)
		if(body != null)
			units.add(body)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, this.scope)
	}
}