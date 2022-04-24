package parsing.ast.general

import linter.Linter
import linter.elements.general.StatementBlock
import linter.scopes.Scope
import source_structure.Position
import util.concretize
import util.indent
import util.toLines

class StatementBlock(start: Position, end: Position, private val statements: List<Element>): Element(start, end) {

	constructor(statement: Element): this(statement.start, statement.end, listOf(statement))

	override fun concretize(linter: Linter, scope: Scope): StatementBlock {
		return StatementBlock(this, statements.concretize(linter, scope))
	}

	override fun toString(): String {
		return "StatementBlock {${statements.toLines().indent()}\n}"
	}
}