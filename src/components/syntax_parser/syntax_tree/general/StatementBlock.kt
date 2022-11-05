package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.general.StatementBlock as SemanticStatementBlockModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import source_structure.Position
import util.concretize
import util.indent
import util.toLines

class StatementBlock(start: Position, end: Position, private val statements: List<Element>): Element(start, end) {

	constructor(statement: Element): this(statement.start, statement.end, listOf(statement))

	override fun concretize(linter: Linter, scope: MutableScope): SemanticStatementBlockModel {
		val blockScope = BlockScope(scope)
		return SemanticStatementBlockModel(this, blockScope, statements.concretize(linter, blockScope))
	}

	override fun toString(): String {
		return "StatementBlock {${statements.toLines().indent()}\n}"
	}
}
