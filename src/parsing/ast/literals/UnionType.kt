package parsing.ast.literals

import errors.internal.CompilerError
import linter.Linter
import linter.elements.literals.AndUnionType
import linter.elements.literals.OrUnionType
import linter.elements.literals.Type as LinterType
import linter.scopes.Scope
import parsing.ast.general.TypeElement
import java.util.*

class UnionType(private val left: TypeElement, private val right: TypeElement, private val mode: Mode): TypeElement(left.start, right.end) {

	override fun concretize(linter: Linter, scope: Scope): LinterType {
		val types = LinkedList<linter.elements.literals.Type>()
		addTypes(linter, scope, types, this)
		return if(mode == Mode.AND)
			AndUnionType(this, types)
		else
			OrUnionType(this, types)
	}

	private fun addTypes(linter: Linter, scope: Scope, types: LinkedList<LinterType>, type: TypeElement) {
		if(type is UnionType && type.mode == mode) {
			addTypes(linter, scope, types, type.left)
			addTypes(linter, scope, types, type.right)
		} else {
			types.add(type.concretize(linter, scope))
		}
	}

	override fun toString(): String {
		return "UnionType { $left ${mode.symbol} $right }"
	}

	enum class Mode(val symbol: String) {
		AND("&"),
		OR("|");

		companion object {

			fun bySymbol(symbol: String): Mode {
				for(mode in values())
					if(mode.symbol == symbol)
						return mode
				throw CompilerError("Failed to parse union type mode '$symbol'.")
			}
		}
	}
}