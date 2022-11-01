package components.compiler.instructions

import errors.internal.CompilerError
import components.compiler.value_analysis.DynamicValue
import components.compiler.value_analysis.HeapValue
import components.compiler.value_analysis.ValueSource

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
