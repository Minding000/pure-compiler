package linting.semantic_model.definitions

import linting.semantic_model.general.Unit
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.BlockScope
import parsing.syntax_tree.definitions.OperatorDefinition as OperatorDefinitionSyntaxTree

class IndexOperatorDefinition(source: OperatorDefinitionSyntaxTree, scope: BlockScope,
							  val indices: List<Parameter>, parameters: List<Parameter>, body: Unit?,
							  returnType: Type?):
	OperatorDefinition(source, "[]", scope, parameters, body, returnType) {

	init {
		units.addAll(indices)
	}

	fun accepts(indexTypes: List<Type?>, parameterTypes: List<Type?>): Boolean {
		if(indices.size != indexTypes.size)
			return false
		if(parameters.size != parameterTypes.size)
			return false
		for(i in indices.indices)
			if(indexTypes[i]?.let { indexType -> indices[i].type?.accepts(indexType) } != true)
				return false
		for(i in parameters.indices)
			if(parameterTypes[i]?.let { parameterType -> parameters[i].type?.accepts(parameterType) } != true)
				return false
		return true
	}

	override fun toString(): String {
		return "[${indices.joinToString { index -> index.type.toString() }}]($variation)"
	}
}