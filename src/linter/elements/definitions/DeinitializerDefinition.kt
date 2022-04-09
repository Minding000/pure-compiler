package linter.elements.definitions

import linter.elements.general.Unit
import parsing.ast.definitions.DeinitializerDefinition

class DeinitializerDefinition(val source: DeinitializerDefinition, val body: Unit?, val isNative: Boolean): Unit() {

	init {
		if(body != null)
			units.add(body)
	}
}