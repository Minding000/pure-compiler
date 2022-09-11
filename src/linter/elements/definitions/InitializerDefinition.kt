package linter.elements.definitions

import linter.Linter
import linter.elements.general.Unit
import linter.elements.literals.ObjectType
import linter.elements.literals.Type
import linter.scopes.BlockScope
import linter.scopes.MutableScope
import linter.scopes.Scope
import java.util.*
import parsing.ast.definitions.InitializerDefinition as ASTInitializerDefinition

class InitializerDefinition(val source: ASTInitializerDefinition, val scope: BlockScope, val parameters: List<Parameter>,
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