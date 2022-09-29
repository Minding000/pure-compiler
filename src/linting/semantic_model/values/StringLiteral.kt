package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.literals.StringLiteral

class StringLiteral(override val source: StringLiteral, val value: String): LiteralValue(source) {

	init {
		val stringType = ObjectType(source, Linter.LiteralType.STRING.className)
		units.add(stringType)
		type = stringType
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.link(Linter.LiteralType.STRING, type)
	}
}