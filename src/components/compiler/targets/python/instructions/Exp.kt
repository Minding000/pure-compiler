package components.compiler.targets.python.instructions

import components.compiler.targets.python.value_analysis.StaticValue
import components.compiler.targets.python.value_analysis.ValueSource

class Exp(leftDynamicValue: ValueSource, rightDynamicValue: ValueSource):
	BinaryInstruction(leftDynamicValue, rightDynamicValue) {

	companion object {

		private fun integerExponentiation(base: Int, exponent: Int): Int {
			var result = 1
			var b = base
			var e = exponent
			while(e != 0) {
				if((e and 1) == 1)
					result *= b
				e = e shr 1
				b *= b
			}
			return result
		}
	}

	init {
		output.setWriteInstruction(this)
	}

	override fun getStaticValue(): StaticValue? {
		val left = leftValueSource
		val right = rightValueSource
		if(left !is StaticValue || right !is StaticValue)
			return null
		return StaticValue(integerExponentiation(left.raw, right.raw))
	}
}
