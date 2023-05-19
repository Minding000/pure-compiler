package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import source_structure.Position
import util.concretize
import util.indent
import util.toLines
import components.semantic_analysis.semantic_model.general.StatementBlock as SemanticStatementBlockModel

class StatementBlock(start: Position, end: Position, private val statements: List<Element>): Element(start, end) {

	constructor(statement: Element): this(statement.start, statement.end, listOf(statement))

	override fun concretize(scope: MutableScope): SemanticStatementBlockModel {
		val blockScope = BlockScope(scope)
		return SemanticStatementBlockModel(this, blockScope, statements.concretize(blockScope))
	}

	override fun toString(): String {
		return "StatementBlock {${statements.toLines().indent()}\n}"
	}
}
