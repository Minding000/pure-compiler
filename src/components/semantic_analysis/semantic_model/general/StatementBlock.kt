package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.semantic_model.scopes.BlockScope
import logger.issues.constant_conditions.UnreachableStatement
import components.syntax_parser.syntax_tree.general.StatementBlock as StatementBlockSyntaxTree

class StatementBlock(override val source: StatementBlockSyntaxTree, public override val scope: BlockScope, val statements: List<Unit>):
	Unit(source, scope) {
	override var isInterruptingExecution = false

	init {
		addUnits(statements)
	}

	override fun validate() {
		super.validate()
		var isCodeReachable = true
		for(statement in statements) {
			if(!isCodeReachable) {
				context.addIssue(UnreachableStatement(statement.source))
				continue
			}
			if(statement.isInterruptingExecution)
				isCodeReachable = false
		}
		isInterruptingExecution = !isCodeReachable
	}
}
