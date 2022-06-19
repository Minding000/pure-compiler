package linter.elements.values

import linter.elements.literals.SimpleType
import parsing.ast.literals.BooleanLiteral

class BooleanLiteral(override val source: BooleanLiteral, val value: Boolean): LiteralValue(source) {

	init {
		val type = SimpleType(source, listOf(), "Bool")
		units.add(type)
		this.type = type
	}
}