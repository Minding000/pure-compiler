package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.control_flow.NextStatement
import errors.internal.CompilerError

class NextStatement(override val model: NextStatement): Unit(model) {

	override fun compile(constructor: LlvmConstructor) {
		val targetLoop = model.targetLoop ?: throw CompilerError(model, "Next statement outside of loop.")
		val errorHandlingContext = model.scope.getSurroundingAlwaysBlock()
		if(errorHandlingContext?.isIn(targetLoop) == true)
			errorHandlingContext.unit.runAlwaysBlock(constructor)
		targetLoop.unit.jumpToNextIteration(constructor)
	}
}
