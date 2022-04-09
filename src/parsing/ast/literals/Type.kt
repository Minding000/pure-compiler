package parsing.ast.literals

import linter.Linter
import linter.elements.literals.Type
import linter.scopes.Scope
import parsing.ast.general.Element
import source_structure.Position

abstract class Type(start: Position, end: Position): Element(start, end) {

	abstract override fun concretize(linter: Linter, scope: Scope): Type
}