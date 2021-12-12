package compiler.instructions

import compiler.value_analysis.DynamicValue
import java.util.*

/**
 *
 * NOTE: Subclasses need to assign 'this' to targetDynamicValue.writeInstruction in their constructor
 */
abstract class WriteInstruction(var targetDynamicValue: DynamicValue): Instruction() {

	override fun getWrittenDynamicValues(): List<DynamicValue> {
		val list = LinkedList<DynamicValue>()
		list.add(targetDynamicValue)
		return list
	}
}