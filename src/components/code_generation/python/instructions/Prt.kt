package components.code_generation.python.instructions

import components.code_generation.python.value_analysis.DynamicValue
import components.code_generation.python.value_analysis.ValueSource
import java.util.*

class Prt(var valueSources: MutableList<ValueSource>): Instruction() {

	override fun getReadDynamicValues(): List<DynamicValue> {
		val registers = LinkedList<DynamicValue>()
		for(valueSource in valueSources)
			if(valueSource is DynamicValue)
				registers.add(valueSource)
		return registers
	}

	override fun replace(current: DynamicValue, new: ValueSource) {
		val iterator = valueSources.listIterator()
		while(iterator.hasNext())
			if(iterator.next() == current)
				iterator.set(new)
	}
}
