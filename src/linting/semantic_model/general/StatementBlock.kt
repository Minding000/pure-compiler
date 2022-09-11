package linting.semantic_model.general

import parsing.syntax_tree.general.StatementBlock

class StatementBlock(val source: StatementBlock, statements: List<Unit>): Unit() {

	init {
		units.addAll(statements)
	}
}