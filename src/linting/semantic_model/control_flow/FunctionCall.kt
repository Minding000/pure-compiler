package linting.semantic_model.control_flow

import linting.Linter
import linting.semantic_model.access.MemberAccess
import linting.semantic_model.definitions.TypeSpecification
import linting.semantic_model.literals.FunctionType
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.StaticType
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import messages.Message
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
			val initializer = functionType.scope.resolveInitializer(parameters)
			if(initializer == null)
				linter.addMessage(source, "Initializer '${getSignature()}' hasn't been declared yet.",
					Message.Type.ERROR)
			else
				type = ObjectType(listOf(), functionType.definition)
		} else if(functionType is FunctionType) {
			try {
				val signature = functionType.resolveSignature(parameters)
				if(signature == null)
					linter.addMessage(source, "The provided values don't match any signature of function '${function.source.getValue()}'.",
						Message.Type.ERROR)
				else
					type = signature.returnType
			} catch(e: FunctionType.SignatureResolutionAmbiguityError) {
				linter.addMessage(source, "Call to function '${getSignature()}' is ambiguous. " +
						"Matching signatures:" + e.signatures.joinToString("\n - ", "\n - "),
					Message.Type.ERROR)
			}
		} else {
			linter.addMessage(source, "'${function.source.getValue()}' is not callable.", Message.Type.ERROR)
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(function is MemberAccess) {
			//TODO continue
			// - check if mutability matches
			// - should mutability be part of the type system? (probably yes)
			//if(function.target.isMutable)
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