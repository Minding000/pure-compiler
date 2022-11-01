package components.parsing.syntax_tree.definitions.sections

import components.parsing.syntax_tree.general.Element
import source_structure.Position

abstract class VariableSectionElement(start: Position, end: Position): Element(start, end) {
	lateinit var parent: VariableSection
}
