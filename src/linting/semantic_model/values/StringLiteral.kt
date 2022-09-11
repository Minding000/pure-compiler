package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.literals.StringLiteral

class StringLiteral(override val source: StringLiteral, val value: String): LiteralValue(source) {

	init {
		val type = ObjectType(source, Linter.Literals.STRING)
		units.add(type)
		this.type = type
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.stringLiteralScope?.let { super.linkTypes(linter, it) }
	}
}