package components.syntax_parser.syntax_tree.general

import source_structure.Position
import util.indent
import util.toLines

class AliasBlock(start: Position, end: Position, val referenceAliases: List<ReferenceAlias>): MetaSyntaxTreeNode(start, end) {

	override fun toString(): String {
		return "AliasBlock {${referenceAliases.toLines().indent()}\n}"
	}
}
