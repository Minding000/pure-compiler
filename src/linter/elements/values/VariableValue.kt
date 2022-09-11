package linter.elements.values

import linter.Linter
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.literals.Identifier

class VariableValue(override val source: Identifier): Value(source) {
	val name = source.getValue()
	var definition: VariableValueDeclaration? = null

	override fun linkValues(linter: Linter, scope: Scope) {
		definition = scope.resolveValue(name)
		if(definition == null)
			linter.messages.add(Message("${source.getStartString()}: Value '$name' hasn't been declared yet.", Message.Type.ERROR))
		type = definition?.type
	}
}