package linter.elements.values

import parsing.ast.literals.BooleanLiteral

class BooleanLiteral(val source: BooleanLiteral, val value: Boolean): LiteralValue()