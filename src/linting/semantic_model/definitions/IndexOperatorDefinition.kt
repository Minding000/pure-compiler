package linting.semantic_model.definitions

import linting.semantic_model.general.Unit
import linting.semantic_model.types.Type
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.values.Value
import parsing.syntax_tree.definitions.OperatorDefinition as OperatorDefinitionSyntaxTree

class IndexOperatorDefinition(source: OperatorDefinitionSyntaxTree, scope: BlockScope, val indices: List<Parameter>,
							  parameters: List<Parameter>, body: Unit?, returnType: Type?):
	OperatorDefinition(source, "[]", scope, parameters, body, returnType) {

	init {
		units.addAll(indices)
	}

	fun accepts(indexValues: List<Value>, parameterValues: List<Value>): Boolean {
		if(indices.size != indexValues.size)
			return false
		if(parameters.size != parameterValues.size)
			return false
		for(indexIndex in indices.indices)
			if(!indexValues[indexIndex].isAssignableTo(indices[indexIndex].type))
				return false
		for(parameterIndex in parameters.indices)
			if(!parameterValues[parameterIndex].isAssignableTo(parameters[parameterIndex].type))
				return false
		return true
	}

	fun isMoreSpecificThan(otherSignature: IndexOperatorDefinition): Boolean {
		if(otherSignature.indices.size != indices.size)
			return false
		if(otherSignature.parameters.size != parameters.size)
			return false
		var areSignaturesEqual = true
		for(indexIndex in indices.indices) {
			val indexType = indices[indexIndex].type ?: return false
			val otherIndexType = otherSignature.indices[indexIndex].type
			if(otherIndexType == null) {
				areSignaturesEqual = false
				continue
			}
			if(otherIndexType != indexType) {
				areSignaturesEqual = false
				if(!otherIndexType.accepts(indexType))
					return false
			}
		}
		for(parameterIndex in parameters.indices) {
			val parameterType = parameters[parameterIndex].type ?: return false
			val otherParameterType = otherSignature.parameters[parameterIndex].type
			if(otherParameterType == null) {
				areSignaturesEqual = false
				continue
			}
			if(otherParameterType != parameterType) {
				areSignaturesEqual = false
				if(!otherParameterType.accepts(parameterType))
					return false
			}
		}
		return !areSignaturesEqual
	}

	override fun toString(): String {
		return "[${indices.joinToString { index -> index.type.toString() }}]($variation)"
	}
}
