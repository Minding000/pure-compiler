package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import util.stringifyTypes
import java.util.LinkedList
import components.syntax_parser.syntax_tree.definitions.OperatorDefinition as OperatorDefinitionSyntaxTree

open class OperatorDefinition(final override val source: OperatorDefinitionSyntaxTree, name: String,
							  val scope: BlockScope, val valueParameters: List<Parameter>, val body: Unit?,
							  returnType: Type?, val isNative: Boolean, val isOverriding: Boolean):
	VariableValueDeclaration(source, name, returnType) {
	val returnType: Type

	init {
		addUnits(body)
		addUnits(valueParameters)
		var type = returnType
		if(type == null) {
			type = ObjectType(source, Linter.LiteralType.NOTHING.className)
			addUnits(type)
		}
		this.returnType = type
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): OperatorDefinition {
		val specificParameters = LinkedList<Parameter>()
		for(parameter in valueParameters)
			specificParameters.add(parameter.withTypeSubstitutions(typeSubstitution))
		return OperatorDefinition(source, name, scope, specificParameters, body,
				returnType.withTypeSubstitutions(typeSubstitution), isNative, isOverriding)
	}

	fun accepts(suppliedValues: List<Value>): Boolean {
		if(valueParameters.size != suppliedValues.size)
			return false
		for(parameterIndex in valueParameters.indices)
			if(!suppliedValues[parameterIndex].isAssignableTo(valueParameters[parameterIndex].type))
				return false
		return true
	}

	fun isMoreSpecificThan(otherSignature: OperatorDefinition): Boolean {
		if(otherSignature.valueParameters.size != valueParameters.size)
			return false
		var areSignaturesEqual = true
		for(parameterIndex in valueParameters.indices) {
			val parameterType = valueParameters[parameterIndex].type ?: return false
			val otherParameterType = otherSignature.valueParameters[parameterIndex].type
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
		return "$name(${valueParameters.stringifyTypes()})"
	}
}
