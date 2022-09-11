package linter.elements.access

import linter.Linter
import linter.elements.values.Value
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.access.Index

class Index(override val source: Index, val target: Value, val indices: List<Value>): Value(source) {

	init {
		units.add(target)
		units.addAll(indices)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		//TODO check if the index operator is taking parameters (is target of an assignment)
		val targetScope = target.type?.scope
		val definition = targetScope?.resolveIndexOperator(indices.map { i -> i.type }, listOf())
		if(definition == null) {
			val name = "[${indices.joinToString { index -> index.type.toString() }}]"
			linter.messages.add(Message("${source.getStartString()}: Operator '$name()' hasn't been declared yet.", Message.Type.ERROR))
		}
		type = definition?.returnType
	}
}