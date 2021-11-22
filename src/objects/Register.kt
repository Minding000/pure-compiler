package objects

open class Register(var index: Int): ValueSource {

	override fun toString(): String {
		return "r$index"
	}
}