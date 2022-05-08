package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.elements.values.VariableValueDeclaration
import parsing.ast.definitions.VariableDeclaration

class VariableDeclaration(override val source: VariableDeclaration, name: String, type: Type?, val value: Unit?,
						  isConstant: Boolean): VariableValueDeclaration(source, name, type, isConstant) {

	init {
		if(type != null)
			units.add(type)
		if(value != null)
			units.add(value)
	}
}