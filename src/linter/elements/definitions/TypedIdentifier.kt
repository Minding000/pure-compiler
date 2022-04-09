package linter.elements.definitions

import linter.elements.literals.Type
import linter.elements.values.VariableValueDeclaration
import parsing.ast.definitions.TypedIdentifier

class TypedIdentifier(override val source: TypedIdentifier, name: String, val type: Type): VariableValueDeclaration(source, name) {

	init {
		units.add(type)
	}
}