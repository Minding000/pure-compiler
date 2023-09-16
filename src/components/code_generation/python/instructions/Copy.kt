package components.code_generation.python.instructions

import components.code_generation.python.value_analysis.DynamicValue
import components.code_generation.python.value_analysis.ValueSource
import java.util.*

class Copy(targetDynamicValue: DynamicValue, var valueSource: ValueSource): WriteInstruction(targetDynamicValue) {

	init {
		targetDynamicValue.setWriteInstruction(this)
	}

	override fun getReadDynamicValues(): List<DynamicValue> {
		val list = LinkedList<DynamicValue>()
		val source = valueSource
		if(source is DynamicValue)
			list.add(source)
		return list
	}

	override fun replace(current: DynamicValue, new: ValueSource) {
		if(new is DynamicValue && targetDynamicValue == current)
			targetDynamicValue = new
		if(valueSource == current)
			valueSource = new
	}
}
