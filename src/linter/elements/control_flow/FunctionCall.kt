package linter.elements.control_flow

import linter.Linter
import linter.elements.literals.SimpleType
import linter.elements.values.Value
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.control_flow.FunctionCall

class FunctionCall(override val source: FunctionCall, val context: Value?, val name: String,
				   val parameters: List<Value>): Value(source) {
	val variation: String
		get() = parameters.joinToString { parameter -> parameter.type.toString()}

	init {
		if(context != null)
			units.add(context)
		units.addAll(parameters)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, scope) //TODO scope is incorrect for parameters
		val initializerType = scope.resolveType(name)
		if(initializerType == null) {
			val definition = scope.resolveFunction(name, parameters.map { p -> p.type })
			if(definition == null)
				linter.messages.add(Message("${source.getStartString()}: Function '$name($variation)' hasn't been declared yet.", Message.Type.ERROR))
			else
				type = definition.returnType
		} else {
			val initializer = initializerType.scope.resolveInitializer(parameters)
			if(initializer == null)
				linter.messages.add(Message("${source.getStartString()}: Initializer '$name($variation)' hasn't been declared yet.", Message.Type.ERROR))
			else
				type = SimpleType(linter, initializerType)
		}
	}
}