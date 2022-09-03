package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.scopes.BlockScope
import parsing.ast.definitions.OperatorDefinition as ASTOperatorDefinition

class IndexOperatorDefinition(source: ASTOperatorDefinition, scope: BlockScope,
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
			if(indexTypes[i]?.let { indices[i].type?.accepts(it) } != true)
				return false
		for(i in parameters.indices)
			if(parameterTypes[i]?.let { parameters[i].type?.accepts(it) } != true)
				return false
		return true
	}

	override fun toString(): String {
		return "[${indices.joinToString { index -> index.type.toString() }}]($variation)"
	}
}