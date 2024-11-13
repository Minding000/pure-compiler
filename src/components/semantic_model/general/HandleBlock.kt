package components.semantic_model.general

import components.code_generation.llvm.models.general.HandleBlock
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.HandleBlock as HandleBlockSyntaxTree

class HandleBlock(override val source: HandleBlockSyntaxTree, scope: Scope, val eventType: Type, val eventVariable: ValueDeclaration?,
				  val block: StatementBlock): SemanticModel(source, scope) {
	override val isInterruptingExecutionBasedOnStructure
		get() = block.isInterruptingExecutionBasedOnStructure
	override val isInterruptingExecutionBasedOnStaticEvaluation
		get() = block.isInterruptingExecutionBasedOnStaticEvaluation

	init {
		addSemanticModels(eventType, eventVariable, block)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(eventVariable != null)
			tracker.declare(eventVariable, true)
		block.analyseDataFlow(tracker)
	}

	override fun toUnit() = HandleBlock(this, eventVariable?.toUnit(), block.toUnit())
}
