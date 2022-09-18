package linting.semantic_model.values

import linting.Linter
import messages.Message
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.literals.Identifier

class VariableValue(override val source: Identifier): Value(source) {
	val name = source.getValue()
	var definition: VariableValueDeclaration? = null

	override fun linkValues(linter: Linter, scope: Scope) {
		definition = scope.resolveValue(name)
		if(definition == null)
			linter.addMessage(source, "Value '$name' hasn't been declared yet.", Message.Type.ERROR)
		type = definition?.type
	}
}