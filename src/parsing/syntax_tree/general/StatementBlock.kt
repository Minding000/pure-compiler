package parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.general.StatementBlock
import linting.semantic_model.scopes.MutableScope
import source_structure.Position
import util.concretize
import util.indent
import util.toLines

class StatementBlock(start: Position, end: Position, private val statements: List<Element>): Element(start, end) {

	constructor(statement: Element): this(statement.start, statement.end, listOf(statement))

	override fun concretize(linter: Linter, scope: MutableScope): StatementBlock {
		return StatementBlock(this, statements.concretize(linter, scope))
	}

	override fun toString(): String {
		return "StatementBlock {${statements.toLines().indent()}\n}"
	}
}