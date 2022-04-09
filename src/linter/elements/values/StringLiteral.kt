package linter.elements.values

import parsing.ast.literals.StringLiteral

class StringLiteral(val source: StringLiteral, val value: String): LiteralValue()