package types

object NativeTypes {
	var INT = NativeType("Int")
	var STRING = NativeType("String")
	var FUNCTION = NativeType("Function")

	fun getByName(name: String): Type? {
		return when(name) {
			INT.name -> INT
			STRING.name -> STRING
			FUNCTION.name -> FUNCTION
			else -> null
		}
	}
}