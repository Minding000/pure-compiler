package components.linting.semantic_model.values

import components.linting.Linter
import components.linting.semantic_model.types.ObjectType
import components.linting.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.literals.StringLiteral as StringLiteralSyntaxTree

class StringLiteral(override val source: StringLiteralSyntaxTree, val value: String): LiteralValue(source) {

	init {
		val stringType = ObjectType(source, Linter.LiteralType.STRING.className)
		units.add(stringType)
		type = stringType
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.link(Linter.LiteralType.STRING, type)
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
}
