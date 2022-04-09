package linter.elements.values

import parsing.ast.literals.NumberLiteral

class NumberLiteral(val source: NumberLiteral, val value: String): LiteralValue()