package components.compiler.targets.python.instructions

import components.compiler.targets.python.value_analysis.DynamicValue
import components.compiler.targets.python.value_analysis.ValueSource
import java.util.*

/**
 *
 * NOTE: Subclasses need to assign 'this' to output.writeInstruction in their constructor
 */
open class BinaryInstruction(var leftValueSource: ValueSource, var rightValueSource: ValueSource): Instruction() {
	var output = DynamicValue()

	override fun getWrittenDynamicValues(): List<DynamicValue> {
		val list = LinkedList<DynamicValue>()
		list.add(output)
		return list
	}

	override fun getReadDynamicValues(): List<DynamicValue> {
		val list = LinkedList<DynamicValue>()
		val left = leftValueSource
		if(left is DynamicValue)
			list.add(left)
		val right = rightValueSource
		if(right is DynamicValue)
			list.add(right)
		return list
	}

	override fun replace(current: DynamicValue, new: ValueSource) {
		if(new is DynamicValue && output == current)
			output = new
		if(leftValueSource == current)
			leftValueSource = new
		if(rightValueSource == current)
			rightValueSource = new
	}
}
