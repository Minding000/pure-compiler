package components.semantic_model.values

import components.semantic_model.context.SpecialType
import components.semantic_model.scopes.Scope
import components.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.literals.StringLiteral as StringLiteralSyntaxTree

class StringLiteral(override val source: StringLiteralSyntaxTree, scope: Scope, val value: String): LiteralValue(source, scope) {

	init {
		type = LiteralType(source, scope, SpecialType.STRING)
		addSemanticModels(type)
	}

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + value.hashCode()
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is StringLiteral)
			return false
		return value == other.value
	}

	override fun toString(): String {
		return "\"$value\""
	}
}
