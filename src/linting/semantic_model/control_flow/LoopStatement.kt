package linting.semantic_model.control_flow

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.Scope
import components.parsing.syntax_tree.control_flow.LoopStatement as LoopStatementSyntaxTree

class LoopStatement(override val source: LoopStatementSyntaxTree, val scope: BlockScope, val generator: Unit?,
					val body: Unit): Unit(source) {

	init {
		if(generator != null)
			units.add(generator)
		units.add(body)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}
}
