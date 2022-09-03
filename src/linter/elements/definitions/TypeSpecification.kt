package linter.elements.definitions

import linter.elements.literals.Type
import linter.elements.values.Value
import parsing.ast.definitions.TypeSpecification as ASTTypeSpecification

class TypeSpecification(override val source: ASTTypeSpecification, baseValue: Value, genericParameters: List<Type>):
	Value(source) {

	init {
		units.add(baseValue)
		units.addAll(genericParameters)
	}
}
