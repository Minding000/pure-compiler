package parsing.ast.definitions.sections

import parsing.ast.general.Element
import source_structure.Position

abstract class VariableSectionElement(start: Position, end: Position): Element(start, end) {
	lateinit var parent: VariableSection
}