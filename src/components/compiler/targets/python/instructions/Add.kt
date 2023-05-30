package components.compiler.targets.python.instructions

import components.compiler.targets.python.value_analysis.StaticValue
import components.compiler.targets.python.value_analysis.ValueSource

class Add(leftDynamicValue: ValueSource, rightDynamicValue: ValueSource, val isNegative: Boolean):
	BinaryInstruction(leftDynamicValue, rightDynamicValue) {

	init {
		output.setWriteInstruction(this)
	}

	override fun getStaticValue(): StaticValue? {
		val left = leftValueSource
		val right = rightValueSource
		if(left !is StaticValue || right !is StaticValue)
			return null
		return StaticValue(if(isNegative) left.raw - right.raw else left.raw + right.raw)
	}
}
