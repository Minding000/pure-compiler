package parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.MutableScope
import source_structure.Position

/**
 * Impacts semantic model directly and returns a type
 */
abstract class TypeElement(start: Position, end: Position): Element(start, end) {

	abstract override fun concretize(linter: Linter, scope: MutableScope): Type
}