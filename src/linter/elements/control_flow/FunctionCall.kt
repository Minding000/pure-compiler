package linter.elements.control_flow

import linter.Linter
import linter.elements.values.Value
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.control_flow.FunctionCall

class FunctionCall(val source: FunctionCall, val context: Value?, val name: String, val parameters: List<Value>): Value() {

	init {
		if(context != null)
			units.add(context)
		units.addAll(parameters)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, scope)
		val variation = parameters.joinToString { parameter -> parameter.type.toString()}
		val definition = scope.resolveFunction(name, variation)
		if(definition == null)
			linter.messages.add(Message("${source.getStartString()}: Function '$name($variation)' hasn't been declared yet.", Message.Type.ERROR))
		type = definition?.returnType
	}
}