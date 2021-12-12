package compiler.value_analysis

class HeapValue(val raw: Any) {

	override fun toString(): String {
		if(raw is String)
			return "\"$raw\""
		return raw.toString()
	}
}