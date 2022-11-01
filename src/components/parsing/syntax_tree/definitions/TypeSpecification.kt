package components.parsing.syntax_tree.definitions

import components.linting.Linter
import components.linting.semantic_model.definitions.TypeSpecification as SemanticTypeSpecificationModel
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.ValueElement
import components.parsing.syntax_tree.literals.TypeList
import source_structure.Position

class TypeSpecification(start: Position, end: Position, private val value: ValueElement,
						private val typeList: TypeList): ValueElement(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticTypeSpecificationModel {
		return SemanticTypeSpecificationModel(this, value.concretize(linter, scope),
			typeList.concretizeTypes(linter, scope))
	}

	override fun toString(): String {
		return "TypeSpecification [ $typeList ] { $value }"
	}
}
