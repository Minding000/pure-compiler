package components.linting.semantic_model.control_flow

import components.linting.Linter
import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.scopes.BlockScope
import components.linting.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.control_flow.LoopStatement as LoopStatementSyntaxTree

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
