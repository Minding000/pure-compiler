package linting.semantic_model.access

import linting.Linter
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import messages.Message
import parsing.syntax_tree.access.IndexAccess as IndexAccessSyntaxTree

class IndexAccess(override val source: IndexAccessSyntaxTree, val target: Value, val indices: List<Value>): Value(source) {
	var sourceExpression: Value? = null

	init {
		units.add(target)
		units.addAll(indices)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		val targetScope = target.type?.scope
		val definition = targetScope?.resolveIndexOperator(indices, listOfNotNull(sourceExpression))
		if(definition == null) {
			val name = "[${indices.joinToString { index -> index.type.toString() }}]"
			linter.addMessage(source,
				"Operator '$name(${sourceExpression?.type ?: ""})' hasn't been declared yet.",
				Message.Type.ERROR)
		} else {
			type = definition.returnType
		}
	}
}
