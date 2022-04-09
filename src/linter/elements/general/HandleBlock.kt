package linter.elements.general

import linter.elements.literals.Type
import linter.elements.values.VariableValueDeclaration
import parsing.ast.general.HandleBlock

class HandleBlock(val source: HandleBlock, val type: Type, val identifier: VariableValueDeclaration?, val block: StatementBlock): Unit() {

	init {
		units.add(type)
		if(identifier != null)
			units.add(identifier)
		units.add(block)
	}
}