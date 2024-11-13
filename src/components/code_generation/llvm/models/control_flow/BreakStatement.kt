package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.control_flow.BreakStatement
import errors.internal.CompilerError

class BreakStatement(override val model: BreakStatement): Unit(model) {

	override fun compile(constructor: LlvmConstructor) {
		val targetLoop = model.targetLoop ?: throw CompilerError(model, "Break statement outside of loop.")
		val errorHandlingContext = model.scope.getSurroundingAlwaysBlock()
		if(errorHandlingContext?.isIn(targetLoop) == true)
			errorHandlingContext.unit.runAlwaysBlock(constructor)
		targetLoop.unit.jumpOut(constructor)
	}
}
