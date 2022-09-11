package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.Type
import linting.semantic_model.values.VariableValueDeclaration
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.Scope
import java.util.LinkedList
import parsing.syntax_tree.definitions.OperatorDefinition as OperatorDefinitionSyntaxTree

open class OperatorDefinition(override val source: OperatorDefinitionSyntaxTree, name: String, val scope: BlockScope,
							  val parameters: List<Parameter>, val body: Unit?, val returnType: Type?):
	VariableValueDeclaration(source, name, returnType, null, true) {
	val variation = parameters.joinToString { parameter -> parameter.type.toString() }

	init {
		units.addAll(parameters)
		if(body != null)
			units.add(body)
		if(returnType != null)
			units.add(returnType)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): OperatorDefinition {
		val specificParameters = LinkedList<Parameter>()
		for(parameter in parameters)
			specificParameters.add(parameter.withTypeSubstitutions(typeSubstitution))
		return OperatorDefinition(source, name, scope, specificParameters, body,
				returnType?.withTypeSubstitutions(typeSubstitution))
	}

	fun accepts(types: List<Type?>): Boolean {
		if(parameters.size != types.size)
			return false
		for(i in parameters.indices)
			if(types[i]?.let { parameters[i].type?.accepts(it) } != true)
				return false
		return true
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}

	override fun toString(): String {
		return "$name($variation)"
	}
}