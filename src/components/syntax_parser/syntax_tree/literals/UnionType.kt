package components.syntax_parser.syntax_tree.literals

import errors.internal.CompilerError
import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.types.AndUnionType as SemanticAndUnionTypeModel
import components.semantic_analysis.semantic_model.types.OrUnionType as SemanticOrUnionTypeModel
import components.semantic_analysis.semantic_model.types.Type as SemanticTypeModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeElement
import java.util.*

class UnionType(private val left: TypeElement, private val right: TypeElement, private val mode: Mode):
	TypeElement(left.start, right.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticTypeModel {
		val types = LinkedList<SemanticTypeModel>()
		addTypes(linter, scope, types, this)
		return if(mode == Mode.AND)
			SemanticAndUnionTypeModel(this, types)
		else
			SemanticOrUnionTypeModel(this, types)
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
