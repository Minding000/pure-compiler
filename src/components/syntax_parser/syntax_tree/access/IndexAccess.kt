package components.syntax_parser.syntax_tree.access

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines
import util.toSemanticTypeModels
import util.toSemanticValueModels
import components.semantic_model.operations.IndexAccess as SemanticIndexAccessModel

class IndexAccess(private val target: ValueSyntaxTreeNode, private val typeParameters: List<TypeSyntaxTreeNode>?,
				  private val indices: List<ValueSyntaxTreeNode>, end: Position): ValueSyntaxTreeNode(target.start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticIndexAccessModel {
		return SemanticIndexAccessModel(this, scope, target.toSemanticModel(scope),
			typeParameters.toSemanticTypeModels(scope), indices.toSemanticValueModels(scope))
	}

	override fun toString(): String {
		var stringRepresentation = "Index [ "
		stringRepresentation += target
		stringRepresentation += " ] {"
		if(typeParameters != null) {
			stringRepresentation += typeParameters.toLines().indent()
			stringRepresentation += ";"
		}
		stringRepresentation += indices.toLines().indent()
		stringRepresentation += "\n}"
		return stringRepresentation
	}
}
