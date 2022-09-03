package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.TypeSpecification
import linter.scopes.MutableScope
import parsing.ast.general.ValueElement
import parsing.ast.literals.TypeList
import source_structure.Position

class TypeSpecification(start: Position, end: Position, private val value: ValueElement,
						private val typeList: TypeList
): ValueElement(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): TypeSpecification {
		return TypeSpecification(this, value.concretize(linter, scope), typeList.concretizeTypes(linter, scope))
	}

	override fun toString(): String {
		return "TypeSpecification [ $typeList ] { $value }"
	}
}