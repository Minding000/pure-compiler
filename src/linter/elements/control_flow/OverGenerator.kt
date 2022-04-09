package linter.elements.control_flow

import linter.elements.general.Unit
import parsing.ast.control_flow.OverGenerator

class OverGenerator(val source: OverGenerator, val collection: Unit, val keyDeclaration: Unit?, val valueDeclaration: Unit): Unit() {

	init {
		units.add(collection)
		if(keyDeclaration != null)
			units.add(keyDeclaration)
		units.add(valueDeclaration)
	}
}