package linting.semantic_model.general

import linting.Linter
import linting.semantic_model.types.Type
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.VariableValueDeclaration
import components.parsing.syntax_tree.general.HandleBlock

class HandleBlock(override val source: HandleBlock, val scope: BlockScope, val eventType: Type,
				  val eventVariable: VariableValueDeclaration?, val block: StatementBlock): Unit(source) {

	init {
		units.add(eventType)
		if(eventVariable != null)
			units.add(eventVariable)
		units.add(block)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}
}
