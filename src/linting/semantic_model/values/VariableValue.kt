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

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + (definition?.hashCode() ?: 0)
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is VariableValue)
			return false
		if(definition == null)
			return false
		return definition == other.definition
	}
}
