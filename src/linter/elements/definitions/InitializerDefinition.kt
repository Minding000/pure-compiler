package linter.elements.definitions

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.BlockScope
import linter.scopes.MutableScope
import linter.scopes.Scope
import parsing.ast.definitions.InitializerDefinition

class InitializerDefinition(val source: InitializerDefinition, val scope: BlockScope, val parameters: List<Parameter>,
							val body: Unit?, val isNative: Boolean): Unit() {
	val variation: String
		get() = parameters.joinToString { parameter -> parameter.type.toString() }

	init {
		units.addAll(parameters)
		if(body != null)
			units.add(body)
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		super.linkPropertyParameters(linter, this.scope)
		scope.declareInitializer(linter, this)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, this.scope)
	}
}