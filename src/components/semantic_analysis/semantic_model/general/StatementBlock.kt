package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.BlockScope
import messages.Message
import components.syntax_parser.syntax_tree.general.StatementBlock as StatementBlockSyntaxTree

class StatementBlock(override val source: StatementBlockSyntaxTree, public override val scope: BlockScope, val statements: List<Unit>):
	Unit(source, scope) {
	override var isInterruptingExecution = false

	init {
		addUnits(statements)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		var isCodeReachable = true
		for(statement in statements) {
			if(isCodeReachable) {
				if(statement.isInterruptingExecution)
					isCodeReachable = false
			} else {
				linter.addMessage(statement.source, "Statement is unreachable.", Message.Type.WARNING)
			}
		}
		isInterruptingExecution = !isCodeReachable
	}
}
