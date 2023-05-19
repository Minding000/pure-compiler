package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import source_structure.Position
import components.semantic_analysis.semantic_model.control_flow.LoopStatement as SemanticLoopStatementModel

class LoopStatement(start: Position, private val generator: Element?, private val body: StatementSection):
	Element(start, body.end) {

	override fun concretize(scope: MutableScope): SemanticLoopStatementModel {
		val loopScope = BlockScope(scope)
		return SemanticLoopStatementModel(this, loopScope, generator?.concretize(loopScope),
			body.concretize(loopScope))
	}

	override fun toString(): String {
		return "Loop${if(generator == null) "" else " [ $generator ]"} { $body }"
	}
}
