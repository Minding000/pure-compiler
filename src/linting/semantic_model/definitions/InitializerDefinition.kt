package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.scopes.Scope
import java.util.*
import parsing.syntax_tree.definitions.InitializerDefinition as InitializerDefinitionSyntaxTree

class InitializerDefinition(val source: InitializerDefinitionSyntaxTree, val scope: BlockScope, val parameters: List<Parameter>,
							val body: Unit?, val isNative: Boolean): Unit() {
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

	fun accepts(types: List<Type?>): Boolean {
		if(parameters.size != types.size)
			return false
		for(i in parameters.indices)
			if(types[i]?.let { parameters[i].type?.accepts(it) } != true)
				return false
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