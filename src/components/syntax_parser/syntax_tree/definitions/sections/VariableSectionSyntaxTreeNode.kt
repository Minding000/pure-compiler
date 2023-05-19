package components.syntax_parser.syntax_tree.definitions.sections

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import source_structure.Position

abstract class VariableSectionSyntaxTreeNode(start: Position, end: Position): SyntaxTreeNode(start, end) {
	lateinit var parent: VariableSection
}
