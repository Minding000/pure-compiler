package components.syntax_parser.syntax_tree.definitions

import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines

class TypeBody(start: Position, end: Position, val members: List<SyntaxTreeNode>): MetaSyntaxTreeNode(start, end) {

	override fun toString(): String {
		return "TypeBody {${members.toLines().indent()}\n}"
	}
}
