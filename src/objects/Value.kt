package objects

class Value(val raw: Any): ValueSource {
	val isInlineable: Boolean = raw is Int

	override fun toString(): String {
		if(raw is String)
			return "\"$raw\""
		return raw.toString()
	}
}