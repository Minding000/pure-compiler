package linter.elements.definitions

import linter.Linter
import linter.elements.literals.Type
import linter.elements.values.Value
import linter.elements.values.VariableValueDeclaration
import linter.scopes.Scope
import parsing.ast.definitions.VariableDeclaration

class VariableDeclaration(override val source: VariableDeclaration, name: String, type: Type?, val value: Value?,
						  isConstant: Boolean): VariableValueDeclaration(source, name, type, isConstant) {

	init {
		if(type != null)
			units.add(type)
		if(value != null)
			units.add(value)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, scope)
		if(type == null)
			type = value?.type
	}
}