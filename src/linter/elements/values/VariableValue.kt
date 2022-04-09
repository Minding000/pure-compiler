package linter.elements.values

import linter.Linter
import linter.scopes.Scope
import parsing.ast.literals.Identifier

class VariableValue(val source: Identifier): Value() {
	val name = source.getValue()
	var definition: VariableValueDeclaration? = null

	override fun linkReferences(linter: Linter, scope: Scope) {
		definition = scope.resolveReference(name)
		println("Link '$name': ${if(definition == null) "failed" else "ok"}")
	}
}