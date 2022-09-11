package linting.semantic_model.access

import linting.Linter
import linting.semantic_model.values.Value
import linting.messages.Message
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.access.Index

class IndexAccess(override val source: Index, val target: Value, val indices: List<Value>): Value(source) {
	var assignedType: Type? = null

	init {
		units.add(target)
		units.addAll(indices)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		val targetScope = target.type?.scope
		val definition = targetScope?.resolveIndexOperator(indices.map { i -> i.type }, listOfNotNull(assignedType))
		if(definition == null) {
			val name = "[${indices.joinToString { index -> index.type.toString() }}]"
			linter.messages.add(Message("${source.getStartString()}: " +
					"Operator '$name(${assignedType ?: ""})' hasn't been declared yet.", Message.Type.ERROR))
		}
		type = definition?.returnType
	}
}