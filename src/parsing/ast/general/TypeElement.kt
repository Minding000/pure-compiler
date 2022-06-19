package parsing.ast.general

import linter.Linter
import linter.elements.literals.Type
import linter.scopes.MutableScope
import source_structure.Position

/**
 * Impacts code flow directly and returns a type
 */
abstract class TypeElement(start: Position, end: Position): Element(start, end) {

	abstract override fun concretize(linter: Linter, scope: MutableScope): Type
}