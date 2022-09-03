package linter.elements.definitions

import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.scopes.TypeScope
import parsing.ast.definitions.TypeAlias as ASTTypeAlias

class TypeAlias(override val source: ASTTypeAlias, name: String, referenceType: Type, scope: TypeScope):
	TypeDefinition(source, name, scope, referenceType)