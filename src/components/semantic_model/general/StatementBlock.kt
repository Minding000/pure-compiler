package components.semantic_model.general

import components.code_generation.llvm.models.general.StatementBlock
import components.semantic_model.context.VariableTracker
import components.semantic_model.scopes.BlockScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.constant_conditions.UnreachableStatement

class StatementBlock(override val source: SyntaxTreeNode, override val scope: BlockScope, val statements: List<SemanticModel>):
	SemanticModel(source, scope) {
	override var isInterruptingExecutionBasedOnStructure = false
	override var isInterruptingExecutionBasedOnStaticEvaluation = false

	init {
		scope.semanticModel = this
		addSemanticModels(statements)
	}

	constructor(source: SyntaxTreeNode, scope: BlockScope, statement: SemanticModel): this(source, scope, listOf(statement))

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		isInterruptingExecutionBasedOnStructure = statements.any(SemanticModel::isInterruptingExecutionBasedOnStructure)
		isInterruptingExecutionBasedOnStaticEvaluation = statements.any(SemanticModel::isInterruptingExecutionBasedOnStaticEvaluation)
	}

	override fun validate() {
		super.validate()
		scope.validate()
		validateUnreachableStatements()
	}

	private fun validateUnreachableStatements() {
		var isCodeReachableBasedOnStaticEvaluation = true
		for(statement in statements) {
			if(!isCodeReachableBasedOnStaticEvaluation) {
				context.addIssue(UnreachableStatement(statement.source))
				continue
			}
			if(statement.isInterruptingExecutionBasedOnStaticEvaluation)
				isCodeReachableBasedOnStaticEvaluation = false
		}
	}

	override fun toUnit() = StatementBlock(this, statements.mapNotNull(SemanticModel::toUnit))
}
