package linter.elements.values

import parsing.ast.literals.Identifier

class LocalVariableDeclaration(override val source: Identifier, isMutable: Boolean = false):
	VariableValueDeclaration(source, source.getValue(), isMutable)