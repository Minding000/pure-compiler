package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.scopes.MutableScope
import source_structure.Position

/**
 * Impacts semantic model directly and returns a type
 */
abstract class TypeElement(start: Position, end: Position): Element(start, end) {

	abstract override fun concretize(linter: Linter, scope: MutableScope): Type
}
