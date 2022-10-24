package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.TypeSpecification
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import parsing.syntax_tree.literals.TypeList
import source_structure.Position

class TypeSpecification(start: Position, end: Position, private val value: ValueElement,
						private val typeList: TypeList): ValueElement(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): TypeSpecification {
		return TypeSpecification(this, value.concretize(linter, scope), typeList.concretizeTypes(linter, scope))
	}

	override fun toString(): String {
		return "TypeSpecification [ $typeList ] { $value }"
	}
}
