package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.Identifier
import components.syntax_parser.syntax_tree.literals.TypeList
import components.semantic_analysis.semantic_model.definitions.TypeSpecification as SemanticTypeSpecificationModel

class TypeSpecification(private val typeList: TypeList, private val identifier: Identifier): ValueElement(typeList.start, identifier.end) {

	override fun concretize(scope: MutableScope): SemanticTypeSpecificationModel {
		return SemanticTypeSpecificationModel(this, scope, typeList.concretizeTypes(scope), identifier.concretize(scope))
	}

	override fun toString(): String {
		return "TypeSpecification [ $typeList ] { $identifier }"
	}
}
