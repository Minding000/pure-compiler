package components.syntax_parser.syntax_tree.control_flow

import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import source_structure.Position
import components.semantic_model.control_flow.LoopStatement as SemanticLoopStatementModel

class LoopStatement(start: Position, private val generator: SyntaxTreeNode?, private val body: StatementSection):
	SyntaxTreeNode(start, body.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticLoopStatementModel {
		val loopScope = BlockScope(scope)
		return SemanticLoopStatementModel(this, loopScope, generator?.toSemanticModel(loopScope),
			body.toSemanticModel(loopScope))
	}

	override fun toString(): String {
		return "Loop${if(generator == null) "" else " [ $generator ]"} { $body }"
	}
}
