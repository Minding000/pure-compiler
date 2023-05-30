package components.compiler.targets.python.value_analysis

open class Register(var index: Int) {

	override fun toString(): String {
		return "r$index"
	}
}
