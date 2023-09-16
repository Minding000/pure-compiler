package components.code_generation.python.value_analysis

class StaticValue(val raw: Int): ValueSource {

	override fun toString(): String {
		return raw.toString()
	}
}
