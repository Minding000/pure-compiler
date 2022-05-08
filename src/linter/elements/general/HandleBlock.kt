package linter.elements.general

import linter.elements.literals.Type
import linter.elements.values.VariableValueDeclaration
import parsing.ast.general.HandleBlock
import parsing.ast.literals.Identifier

class HandleBlock(val source: HandleBlock, val eventType: Type, val identifier: Identifier?,
				  val block: StatementBlock): Unit() {
	val eventVariable = if(identifier == null)
		null
	else
		VariableValueDeclaration(identifier, identifier.getValue(), eventType, true)

	init {
		units.add(eventType)
		if(eventVariable != null)
			units.add(eventVariable)
		units.add(block)
	}
}