package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import source_structure.Position

/**
 * Impacts semantic model directly and returns a type
 */
abstract class TypeSyntaxTreeNode(start: Position, end: Position): SyntaxTreeNode(start, end) {

	abstract override fun toSemanticModel(scope: MutableScope): Type
}
