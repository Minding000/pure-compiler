package compiler.instructions

import compiler.value_analysis.DynamicValue
import compiler.value_analysis.StaticValue
import compiler.value_analysis.ValueSource
import java.util.*

abstract class Instruction {

	fun getDynamicValues(): List<DynamicValue> {
		val list = LinkedList<DynamicValue>()
		list.addAll(getWrittenDynamicValues())
		list.addAll(getReadDynamicValues())
		return list
	}

	open fun getWrittenDynamicValues(): List<DynamicValue> {
		return LinkedList()
	}

	open fun getReadDynamicValues(): List<DynamicValue> {
		return LinkedList()
	}

	fun writesTo(dynamicValue: DynamicValue): Boolean {
		return getWrittenDynamicValues().contains(dynamicValue)
	}

	/**
	 * Computes the value of this instruction if possible
	 */
	open fun getStaticValue(): StaticValue? {
		return null
	}

	abstract fun replace(current: DynamicValue, new: ValueSource)
}