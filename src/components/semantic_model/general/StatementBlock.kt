package components.semantic_model.general

import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.control_flow.BreakStatement
import components.semantic_model.control_flow.NextStatement
import components.semantic_model.control_flow.ReturnStatement
import components.semantic_model.scopes.BlockScope
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
		scope.validate()
		validateUnreachableStatements()
	}

	private fun validateUnreachableStatements() {
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

	override fun compile(constructor: LlvmConstructor) {
		for(statement in statements) {
			statement.compile(constructor)
			if(statement is BreakStatement || statement is NextStatement || statement is ReturnStatement)
				break
		}
	}
}
