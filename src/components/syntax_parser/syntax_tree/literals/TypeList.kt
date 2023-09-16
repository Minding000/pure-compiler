package components.syntax_parser.syntax_tree.literals

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines
import util.toSemanticTypeModels
import components.semantic_model.types.Type as SemanticTypeModel

class TypeList(private val typeParameters: List<TypeSyntaxTreeNode>, start: Position, end: Position): MetaSyntaxTreeNode(start, end) {

	fun toSemanticModels(scope: MutableScope): List<SemanticTypeModel> {
		return typeParameters.toSemanticTypeModels(scope)
	}

	override fun toString(): String {
		return "TypeList {${typeParameters.toLines().indent()}\n}"
	}
}
