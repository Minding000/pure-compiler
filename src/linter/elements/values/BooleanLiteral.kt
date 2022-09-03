package linter.elements.values

import linter.Linter
import linter.elements.literals.ObjectType
import linter.scopes.Scope
import parsing.ast.literals.BooleanLiteral

class BooleanLiteral(override val source: BooleanLiteral, val value: Boolean): LiteralValue(source) {

	init {
		val type = ObjectType(source, listOf(), "Bool")
		units.add(type)
		this.type = type
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.booleanLiteralScope?.let { super.linkTypes(linter, it) }
	}
}