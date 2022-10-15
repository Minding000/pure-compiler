package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.Type
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import java.util.*
import parsing.syntax_tree.definitions.InitializerDefinition as InitializerDefinitionSyntaxTree

class InitializerDefinition(override val source: InitializerDefinitionSyntaxTree, val scope: BlockScope,
							val parameters: List<Parameter>, val body: Unit?, val isNative: Boolean): Unit(source) {
	val variation: String
		get() = parameters.joinToString { parameter -> parameter.type.toString() }

	init {
		units.addAll(parameters)
		if(body != null)
			units.add(body)
	}

	fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): InitializerDefinition {
		val specificParameters = LinkedList<Parameter>()
		for(parameter in parameters)
			specificParameters.add(parameter.withTypeSubstitutions(typeSubstitution))
		return InitializerDefinition(source, scope, specificParameters, body, isNative)
	}

	fun accepts(suppliedValues: List<Value>): Boolean {
		if(parameters.size != suppliedValues.size)
			return false
		for(parameterIndex in parameters.indices) {
			if(!suppliedValues[parameterIndex].isAssignableTo(parameters[parameterIndex].type))
				return false
		}
		return true
	}

	fun isMoreSpecificThan(otherInitializerDefinition: InitializerDefinition): Boolean {
		if(parameters.size != otherInitializerDefinition.parameters.size)
			return false
		var hasSameSpecificity = true
		for(parameterIndex in parameters.indices) {
			val parameterType = parameters[parameterIndex].type
			val otherParameterType = otherInitializerDefinition.parameters[parameterIndex].type
			if(parameterType != otherParameterType) {
				hasSameSpecificity = false
				break
			}
		}
		if(hasSameSpecificity)
			return false
		for(parameterIndex in parameters.indices) {
			val parameterType = parameters[parameterIndex].type ?: return false
			val otherParameterType = otherInitializerDefinition.parameters[parameterIndex].type ?: continue
			if(!otherParameterType.accepts(parameterType))
				return false
		}
		return true
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		super.linkPropertyParameters(linter, this.scope)
		scope.declareInitializer(linter, this)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}
}
