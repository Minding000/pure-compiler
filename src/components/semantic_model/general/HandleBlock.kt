package components.semantic_model.general

import components.code_generation.llvm.LlvmBlock
import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.HandleBlock as HandleBlockSyntaxTree

class HandleBlock(override val source: HandleBlockSyntaxTree, scope: Scope, val eventType: Type, val eventVariable: ValueDeclaration?,
				  val block: StatementBlock): SemanticModel(source, scope) {
	private lateinit var entryBlock: LlvmBlock
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

	override fun compile(constructor: LlvmConstructor) {
		val function = constructor.getParentFunction()
		entryBlock = constructor.createBlock(function, "handle_block_entry")
		constructor.select(entryBlock)
		val exceptionParameter = context.getExceptionParameter(constructor, function)
		//TODO write exception into 'eventVariable'
		constructor.buildStore(constructor.nullPointer, exceptionParameter)
		block.compile(constructor)
	}

	fun jumpTo(constructor: LlvmConstructor) {
		constructor.buildJump(entryBlock)
	}
}
