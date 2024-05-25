package components.syntax_parser.syntax_tree.literals

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import errors.internal.CompilerError
import java.util.*
import components.semantic_model.types.AndUnionType as SemanticAndUnionTypeModel
import components.semantic_model.types.OrUnionType as SemanticOrUnionTypeModel
import components.semantic_model.types.Type as SemanticTypeModel

class UnionType(private val left: TypeSyntaxTreeNode, private val right: TypeSyntaxTreeNode, private val mode: Mode):
	TypeSyntaxTreeNode(left.start, right.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticTypeModel {
		val types = LinkedList<SemanticTypeModel>()
		addTypes(scope, types, this)
		return if(mode == Mode.AND)
			SemanticAndUnionTypeModel(this, scope, types)
		else
			SemanticOrUnionTypeModel(this, scope, types)
	}

	private fun addTypes(scope: MutableScope, types: LinkedList<SemanticTypeModel>, type: TypeSyntaxTreeNode) {
		if(type is UnionType && type.mode == mode) {
			addTypes(scope, types, type.left)
			addTypes(scope, types, type.right)
		} else {
			types.add(type.toSemanticModel(scope))
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
				for(mode in entries)
					if(mode.symbol == symbol)
						return mode
				throw CompilerError("Failed to parse union type mode '$symbol'.")
			}
		}
	}
}
