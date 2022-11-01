package components.linting.semantic_model.values

import components.linting.Linter
import components.linting.semantic_model.scopes.Scope
import messages.Message
import components.parsing.syntax_tree.literals.Identifier

class VariableValue(override val source: Identifier): Value(source) {
	val name = source.getValue()
	var definition: VariableValueDeclaration? = null

	override fun linkValues(linter: Linter, scope: Scope) {
		val definition = scope.resolveValue(name)
		if(definition == null) {
			linter.addMessage(source, "Value '$name' hasn't been declared yet.", Message.Type.ERROR)
			return
		}
		this.definition = definition
		type = definition.type
		if(definition.isConstant)
			staticValue = definition.value?.staticValue
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
