package linting.semantic_model.control_flow

import linting.Linter
import linting.semantic_model.access.MemberAccess
import linting.semantic_model.definitions.TypeSpecification
import linting.semantic_model.literals.FunctionType
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.StaticType
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import linting.messages.Message
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.control_flow.FunctionCall

class FunctionCall(override val source: FunctionCall, val function: Value, val parameters: List<Value>): Value(source) {

	init {
		units.add(function)
		units.addAll(parameters)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		val functionType = function.type
		if(functionType is StaticType) {
			val initializer = functionType.scope.resolveInitializer(parameters.map { p -> p.type })
			if(initializer == null)
				linter.messages.add(Message("${source.getStartString()}: " +
						"Initializer '${getSignature()}' hasn't been declared yet.", Message.Type.ERROR))
			else
				type = ObjectType(listOf(), functionType.definition)
		} else if(functionType is FunctionType) {
			try {
				val signature = functionType.resolveSignature(parameters.map { p -> p.type })
				if(signature == null)
					linter.messages.add(Message("${source.getStartString()}: " +
							"The provided values don't match any signature of function '${function.source.getValue()}'.",
						Message.Type.ERROR))
				else
					type = signature.returnType
			} catch(e: FunctionType.SignatureResolutionAmbiguityError) {
				linter.messages.add(Message("${source.getStartString()}: " +
						"Call to function '${getSignature()}' is ambiguous. " +
						"Matching signatures:" +
						e.signatures.joinToString("\n - ", "\n - "), Message.Type.ERROR))
			}
		} else {
			linter.messages.add(Message("${source.getStartString()}: " +
					"'${function.source.getValue()}' is not callable.", Message.Type.ERROR))
		}
	}

	private fun getSignature(): String {
		var signature = ""
		var function = function
		if(function is TypeSpecification)
			function = function.baseValue
		signature += when(function) {
			is VariableValue -> function.name
			is MemberAccess -> "${function.target.type}.${function.member.name}"
			else -> "<anonymous function>"
		}
		signature += "("
		signature += parameters.joinToString { parameter -> parameter.type.toString() }
		signature += ")"
		return signature
	}
}