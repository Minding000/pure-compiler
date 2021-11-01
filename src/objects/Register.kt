package objects

open class Register(val index: Int) {

	override fun toString(): String {
		return "r$index"
	}
}