package components.semantic_model.general

import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.control_flow.BreakStatement
import components.semantic_model.control_flow.NextStatement
import components.semantic_model.control_flow.ReturnStatement
import components.semantic_model.scopes.BlockScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.constant_conditions.UnreachableStatement

class StatementBlock(override val source: SyntaxTreeNode, override val scope: BlockScope, val statements: List<SemanticModel>):
	SemanticModel(source, scope) {
	override var isInterruptingExecution = false

	init {
		addSemanticModels(statements)
	}

	constructor(source: SyntaxTreeNode, scope: BlockScope, statement: SemanticModel): this(source, scope, listOf(statement))

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
			//TODO also break, if statement never returns (e.g. if statement) based on LLVM rules, not static evaluation
			if(statement is BreakStatement || statement is NextStatement || statement is ReturnStatement)
				break
		}
	}
}
