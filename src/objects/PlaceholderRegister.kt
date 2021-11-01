package objects

import errors.CompilerError
import java.lang.Exception

class PlaceholderRegister: Register(-1) {

	override fun toString(): String {
		throw CompilerError("Placeholder register used.")
	}
}