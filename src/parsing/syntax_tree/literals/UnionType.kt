package parsing.syntax_tree.literals

import errors.internal.CompilerError
import linting.Linter
import linting.semantic_model.literals.AndUnionType
import linting.semantic_model.literals.OrUnionType
import linting.semantic_model.literals.Type as SemanticTypeModel
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.TypeElement
import java.util.*

class UnionType(private val left: TypeElement, private val right: TypeElement, private val mode: Mode): TypeElement(left.start, right.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticTypeModel {
		val types = LinkedList<linting.semantic_model.literals.Type>()
		addTypes(linter, scope, types, this)
		return if(mode == Mode.AND)
			AndUnionType(this, types)
		else
			OrUnionType(this, types)
	}

	private fun addTypes(linter: Linter, scope: MutableScope, types: LinkedList<SemanticTypeModel>, type: TypeElement) {
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