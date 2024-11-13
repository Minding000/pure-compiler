package components.code_generation.llvm.models.general

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.general.StatementBlock

class StatementBlock(override val model: StatementBlock, val statements: List<Unit>): Unit(model, statements) {

	override fun compile(constructor: LlvmConstructor) {
		for(statement in statements) {
			statement.compile(constructor)
			if(statement.model.isInterruptingExecutionBasedOnStructure)
				break
		}
	}
}
