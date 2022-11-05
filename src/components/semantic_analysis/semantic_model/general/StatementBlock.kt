package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import messages.Message
import components.syntax_parser.syntax_tree.general.StatementBlock as StatementBlockSyntaxTree

class StatementBlock(override val source: StatementBlockSyntaxTree, val scope: BlockScope, val statements: List<Unit>):
	Unit(source) {
	override var isInterruptingExecution = false

	init {
		addUnits(statements)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
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
