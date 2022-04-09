package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.literals.Type
import parsing.ast.definitions.VariableDeclaration

class VariableDeclaration(val source: VariableDeclaration, val name: String, val type: Type?, val value: Unit?, val isConstant: Boolean): Unit() {

	init {
		if(type != null)
			units.add(type)
		if(value != null)
			units.add(value)
	}
}