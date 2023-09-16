package components.code_generation.python.instructions

import components.code_generation.python.value_analysis.DynamicValue
import components.code_generation.python.value_analysis.HeapValue
import components.code_generation.python.value_analysis.ValueSource
import errors.internal.CompilerError

class Load(targetDynamicValue: DynamicValue, val heapValue: HeapValue): WriteInstruction(targetDynamicValue) {

	init {
		this.targetDynamicValue.setWriteInstruction(this)
	}

	override fun replace(current: DynamicValue, new: ValueSource) {
		if(new !is DynamicValue)
			throw CompilerError("Cannot assign to static value in load instruction.")
		if(targetDynamicValue == current)
			targetDynamicValue = new
	}
}
