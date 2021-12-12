package compiler.instructions

import compiler.value_analysis.StaticValue
import compiler.value_analysis.ValueSource

class Mul(leftDynamicValue: ValueSource, rightDynamicValue: ValueSource, val isDivision: Boolean):
	BinaryInstruction(leftDynamicValue, rightDynamicValue) {

	init {
		output.setWriteInstruction(this)
	}

	override fun getStaticValue(): StaticValue? {
		val left = leftValueSource
		val right = rightValueSource
		if(left !is StaticValue || right !is StaticValue)
			return null
		return StaticValue(if(isDivision) left.raw / right.raw else left.raw * right.raw)
	}
}
