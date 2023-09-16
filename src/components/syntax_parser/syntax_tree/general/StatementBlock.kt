package components.syntax_parser.syntax_tree.general

import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.MutableScope
import source_structure.Position
import util.indent
import util.toLines
import util.toSemanticModels
import components.semantic_model.general.StatementBlock as SemanticStatementBlockModel

class StatementBlock(start: Position, end: Position, private val statements: List<SyntaxTreeNode>): SyntaxTreeNode(start, end) {

	constructor(statement: SyntaxTreeNode): this(statement.start, statement.end, listOf(statement))

	override fun toSemanticModel(scope: MutableScope): SemanticStatementBlockModel {
		val blockScope = BlockScope(scope)
		return SemanticStatementBlockModel(this, blockScope, statements.toSemanticModels(blockScope))
	}

	override fun toString(): String {
		return "StatementBlock {${statements.toLines().indent()}\n}"
	}
}
