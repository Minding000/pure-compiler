package linter.elements.values

import linter.Linter
import linter.elements.literals.ObjectType
import linter.scopes.Scope
import parsing.ast.literals.StringLiteral

class StringLiteral(override val source: StringLiteral, val value: String): LiteralValue(source) {

	init {
		val type = ObjectType(source, listOf(), "String")
		units.add(type)
		this.type = type
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.stringLiteralScope?.let { super.linkTypes(linter, it) }
	}
}