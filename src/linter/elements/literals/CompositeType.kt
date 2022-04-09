package linter.elements.literals

import linter.elements.general.Unit
import linter.elements.values.TypeDefinition
import parsing.ast.literals.QuantifiedType

class CompositeType(val source: QuantifiedType, val baseType: TypeDefinition,
					val genericTypes: List<Type>): Unit() {

	init {
		units.add(baseType)
		units.addAll(genericTypes)
	}
}