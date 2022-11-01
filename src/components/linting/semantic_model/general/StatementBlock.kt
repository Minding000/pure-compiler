package components.linting.semantic_model.general

import components.linting.Linter
import messages.Message
import components.syntax_parser.syntax_tree.general.StatementBlock as StatementBlockSyntaxTree

class StatementBlock(override val source: StatementBlockSyntaxTree, val statements: List<Unit>): Unit(source) {
	override var isInterruptingExecution = false

	init {
		units.addAll(statements)
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
