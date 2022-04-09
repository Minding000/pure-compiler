package linter.elements.general

import parsing.ast.general.StatementBlock

class StatementBlock(val source: StatementBlock, statements: List<Unit>): Unit() {

	init {
		units.addAll(statements)
	}
}