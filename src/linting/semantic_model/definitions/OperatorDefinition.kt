package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.Type
import linting.semantic_model.values.VariableValueDeclaration
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import java.util.LinkedList
import parsing.syntax_tree.definitions.OperatorDefinition as OperatorDefinitionSyntaxTree

open class OperatorDefinition(override val source: OperatorDefinitionSyntaxTree, name: String,
							  val scope: BlockScope, val parameters: List<Parameter>, val body: Unit?,
							  returnType: Type?):
	VariableValueDeclaration(source, name, returnType ?: ObjectType(source, Linter.LiteralType.NOTHING.className)) {
	val returnType = type!!
	val variation = parameters.joinToString { parameter -> parameter.type.toString() }

	init {
		units.addAll(parameters)
		if(body != null)
			units.add(body)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): OperatorDefinition {
		val specificParameters = LinkedList<Parameter>()
		for(parameter in parameters)
			specificParameters.add(parameter.withTypeSubstitutions(typeSubstitution))
		return OperatorDefinition(source, name, scope, specificParameters, body,
				returnType.withTypeSubstitutions(typeSubstitution))
	}

	fun accepts(suppliedValues: List<Value>): Boolean {
		if(parameters.size != suppliedValues.size)
			return false
		for(parameterIndex in parameters.indices)
			if(!suppliedValues[parameterIndex].isAssignableTo(parameters[parameterIndex].type))
				return false
		return true
	}

	fun isMoreSpecificThan(otherSignature: OperatorDefinition): Boolean {
		if(otherSignature.parameters.size != parameters.size)
			return false
		var areSignaturesEqual = true
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

	override fun linkTypes(linter: Linter, scope: Scope) {
		if(Linter.LiteralType.NOTHING.matches(returnType)) {
			for(unit in units)
				if(unit != returnType)
					unit.linkTypes(linter, this.scope)
			linter.link(Linter.LiteralType.NOTHING, returnType)
		} else {
			super.linkTypes(linter, this.scope)
		}
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}

	override fun toString(): String {
		return "$name($variation)"
	}
}
