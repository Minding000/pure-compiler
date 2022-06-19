package linter.elements.access

import linter.Linter
import linter.elements.values.Value
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.access.Index

class Index(val source: Index, val target: Value, val indices: List<Value>): Value() {

	init {
		units.add(target)
		units.addAll(indices)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, scope)
		val name = "[${indices.joinToString { index -> index.type.toString() }}]"
		val variation = "" //TODO check if the index operator is taking parameters (is target of an assignment)
		val definition = target.type?.scope?.resolveOperator(name, variation)
		if(definition == null)
			linter.messages.add(Message("${source.getStartString()}: Operator '$name($variation)' hasn't been declared yet.", Message.Type.ERROR))
		type = definition?.returnType
	}
}