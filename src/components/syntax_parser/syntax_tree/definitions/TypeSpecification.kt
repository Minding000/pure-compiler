package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import components.syntax_parser.syntax_tree.literals.TypeList
import components.semantic_analysis.semantic_model.definitions.TypeSpecification as SemanticTypeSpecificationModel

class TypeSpecification(private val typeList: TypeList, private val identifier: Identifier): ValueSyntaxTreeNode(typeList.start, identifier.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticTypeSpecificationModel {
		return SemanticTypeSpecificationModel(this, scope, typeList.toSemanticModels(scope), identifier.toSemanticModel(scope))
	}

	override fun toString(): String {
		return "TypeSpecification [ $typeList ] { $identifier }"
	}
}
