package linting.semantic_model.general

import linting.semantic_model.literals.Type
import linting.semantic_model.values.VariableValueDeclaration
import parsing.syntax_tree.general.HandleBlock
import parsing.syntax_tree.literals.Identifier

class HandleBlock(val source: HandleBlock, val eventType: Type, val identifier: Identifier?,
				  val block: StatementBlock): Unit() {
	val eventVariable = if(identifier == null)
		null
	else
		VariableValueDeclaration(identifier, identifier.getValue(), eventType, null, true)

	init {
		units.add(eventType)
		if(eventVariable != null)
			units.add(eventVariable)
		units.add(block)
	}
}