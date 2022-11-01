package components.parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.LoopStatement as SemanticLoopStatementModel
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.StatementSection
import source_structure.Position

class LoopStatement(start: Position, private val generator: Element?, private val body: StatementSection):
	Element(start, body.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticLoopStatementModel {
		val loopScope = BlockScope(scope)
		return SemanticLoopStatementModel(this, loopScope, generator?.concretize(linter, loopScope),
			body.concretize(linter, loopScope))
	}

	override fun toString(): String {
		return "Loop${if(generator == null) "" else " [ $generator ]"} { $body }"
	}
}
