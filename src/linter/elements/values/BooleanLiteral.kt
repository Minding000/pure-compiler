package linter.elements.values

import linter.elements.literals.SimpleType
import parsing.ast.literals.BooleanLiteral

class BooleanLiteral(val source: BooleanLiteral, val value: Boolean): LiteralValue() {

	init {
		val type = SimpleType(source, listOf(), "Bool")
		units.add(type)
		this.type = type
	}
}