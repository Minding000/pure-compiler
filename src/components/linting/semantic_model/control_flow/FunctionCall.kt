package components.linting.semantic_model.control_flow

import errors.user.SignatureResolutionAmbiguityError
import components.linting.Linter
import components.linting.semantic_model.definitions.TypeSpecification
import components.linting.semantic_model.operations.MemberAccess
import components.linting.semantic_model.scopes.Scope
import components.linting.semantic_model.types.FunctionType
import components.linting.semantic_model.types.ObjectType
import components.linting.semantic_model.types.StaticType
import components.linting.semantic_model.types.Type
import components.linting.semantic_model.values.Value
import components.linting.semantic_model.values.VariableValue
import messages.Message
import components.parsing.syntax_tree.control_flow.FunctionCall as FunctionCallSyntaxTree
import util.stringifyTypes

class FunctionCall(override val source: FunctionCallSyntaxTree, val function: Value, val typeParameters: List<Type>,
				   val valueParameters: List<Value>): Value(source) {

	init {
		staticValue = this
		units.add(function)
		units.addAll(typeParameters)
		units.addAll(valueParameters)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		when(val targetType = function.type) {
			is StaticType -> resolveInitializerCall(linter, targetType)
			is FunctionType -> resolveFunctionCall(linter, targetType)
			else -> linter.addMessage(source, "'${function.source.getValue()}' is not callable.",
				Message.Type.ERROR)
		}
	}

	private fun resolveInitializerCall(linter: Linter, targetType: StaticType) {
		val genericDefinitionTypes = (targetType.definition.baseDefinition ?: targetType.definition).scope
			.getGenericTypeDefinitions()
		val definitionTypeParameters = (function as? TypeSpecification)?.typeParameters ?: listOf()
		try {
			val match = targetType.scope.resolveInitializer(genericDefinitionTypes, definitionTypeParameters,
				typeParameters, valueParameters)
			if(match == null) {
				linter.addMessage(source, "Initializer '${getSignature()}' hasn't been declared yet.",
					Message.Type.ERROR)
				return
			}
			val type = ObjectType(match.definitionTypeSubstitutions.map { typeSubstitution -> typeSubstitution.value },
				targetType.definition)
			type.resolveGenerics(linter)
			units.add(type)
			this.type = type
		} catch(error: SignatureResolutionAmbiguityError) {
			linter.addMessage(source, "Call to initializer '${getSignature()}' is ambiguous. " +
				"Matching signatures:" + error.signatures.joinToString("\n - ", "\n - "),
				Message.Type.ERROR) //TODO write test for this
		}
	}

	private fun resolveFunctionCall(linter: Linter, functionType: FunctionType) {
		try {
			val signature = functionType.resolveSignature(typeParameters, valueParameters)
			if(signature == null) {
				var parameterStringRepresentation = ""
				if(typeParameters.isNotEmpty()) {
					parameterStringRepresentation += typeParameters.joinToString()
					parameterStringRepresentation += ";"
					if(valueParameters.isNotEmpty())
						parameterStringRepresentation += " "
				}
				parameterStringRepresentation += valueParameters.stringifyTypes()
				linter.addMessage(source,
					"The provided parameters ($parameterStringRepresentation) don't match any signature " +
						"of function '${function.source.getValue()}'.", Message.Type.ERROR)
				return
			}
			type = signature.returnType
		} catch(error: SignatureResolutionAmbiguityError) {
			linter.addMessage(source, "Call to function '${getSignature()}' is ambiguous. " +
				"Matching signatures:" + error.signatures.joinToString("\n - ", "\n - "),
				Message.Type.ERROR)
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
		signature += valueParameters.joinToString { parameter -> parameter.type.toString() }
		signature += ")"
		return signature
	}
}
