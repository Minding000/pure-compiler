package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.Class
import components.semantic_analysis.semantic_model.definitions.TypeSpecification
import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import errors.user.SignatureResolutionAmbiguityError
import messages.Message
import util.stringifyTypes
import components.syntax_parser.syntax_tree.control_flow.FunctionCall as FunctionCallSyntaxTree

class FunctionCall(override val source: FunctionCallSyntaxTree, val function: Value, val typeParameters: List<Type>,
				   val valueParameters: List<Value>): Value(source) {

	init {
		staticValue = this
		addUnits(function)
		addUnits(typeParameters, valueParameters)
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
		(targetType.definition as? Class)?.let { `class` ->
			if(`class`.isAbstract)
				linter.addMessage(source, "Abstract class '${`class`.name}' cannot be instantiated.",
					Message.Type.ERROR)
		}
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
			addUnits(type)
			this.type = type
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(linter, source, "initializer", getSignature())
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
			error.log(linter, source, "function", getSignature())
		}
	}

	private fun getSignature(): String {
		var signature = ""
		val function = function
		signature += when(function) {
			is VariableValue -> function.name
			is TypeSpecification -> function
			is MemberAccess -> "${function.target.type}.${function.member}"
			else -> "<anonymous function>"
		}
		signature += "("
		if(typeParameters.isNotEmpty()) {
			signature += typeParameters.joinToString()
			signature += ";"
			if(valueParameters.isNotEmpty())
				signature += " "
		}
		signature += valueParameters.joinToString { parameter -> parameter.type.toString() }
		signature += ")"
		return signature
	}
}
