package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.semantic_model.scopes.BlockScope
import logger.issues.constant_conditions.UnreachableStatement
import components.syntax_parser.syntax_tree.general.StatementBlock as StatementBlockSyntaxTree

class StatementBlock(override val source: StatementBlockSyntaxTree, override val scope: BlockScope, val statements: List<SemanticModel>):
	SemanticModel(source, scope) {
	override var isInterruptingExecution = false

	init {
		addSemanticModels(statements)
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
