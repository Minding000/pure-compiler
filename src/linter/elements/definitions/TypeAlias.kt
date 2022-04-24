package linter.elements.definitions

import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import parsing.ast.definitions.TypeAlias as ASTTypeAlias

class TypeAlias(override val source: ASTTypeAlias, name: String, referenceType: Type):
	TypeDefinition(source, name, referenceType, false) {

	init {
		units.add(referenceType)
	}
}