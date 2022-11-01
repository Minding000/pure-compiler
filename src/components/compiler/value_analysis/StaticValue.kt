package components.compiler.value_analysis

class StaticValue(val raw: Int): ValueSource {

	override fun toString(): String {
		return raw.toString()
	}
}
