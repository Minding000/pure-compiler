package linter.elements.values

import linter.elements.literals.SimpleType
import parsing.ast.literals.StringLiteral

class StringLiteral(val source: StringLiteral, val value: String): LiteralValue() {

	init {
		val type = SimpleType(source, listOf(), "String")
		units.add(type)
		this.type = type
	}
}