package linting.semantic_model.literals

import linting.Linter
import linting.semantic_model.definitions.OperatorDefinition
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration
import parsing.syntax_tree.general.Element

class QuantifiedType(val source: Element, val baseType: Type, val hasDynamicQuantity: Boolean,
					 val isOptional: Boolean): Type() {

	init {
		units.add(baseType)
		if(!hasDynamicQuantity)
			baseType.scope.subscribe(this)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): QuantifiedType {
		return QuantifiedType(source, baseType.withTypeSubstitutions(typeSubstitution), hasDynamicQuantity, isOptional)
	}

	override fun onNewType(type: TypeDefinition) {
		//TODO mind optionality
		this.scope.addType(type)
	}

	override fun onNewValue(value: VariableValueDeclaration) {
		//TODO mind optionality
		this.scope.addValue(value)
	}

	override fun onNewOperator(operator: OperatorDefinition) {
		//TODO mind optionality
		this.scope.addOperator(operator)
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		if(hasDynamicQuantity)
			return false
		val sourceType = resolveTypeAlias(unresolvedSourceType)
		if(sourceType is QuantifiedType) {
			if(sourceType.isOptional && !isOptional)
				return false
		}
		return baseType.accepts(sourceType)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		if(hasDynamicQuantity)
			return false
		val targetType = resolveTypeAlias(unresolvedTargetType)
		if(targetType is QuantifiedType) {
			if(isOptional && !targetType.isOptional)
				return false
		}
		return baseType.isAssignableTo(targetType)
	}

	override fun getKeyType(linter: Linter): Type? {
		if(!hasDynamicQuantity)
			return super.getKeyType(linter)
		return ObjectType(source, "Int")
	}

	override fun getValueType(linter: Linter): Type? {
		if(!hasDynamicQuantity)
			return super.getValueType(linter)
		return baseType
	}

	override fun equals(other: Any?): Boolean {
		if(other !is QuantifiedType)
			return false
		if(baseType != other.baseType)
			return false
		if(hasDynamicQuantity != other.hasDynamicQuantity)
			return false
		if(isOptional != other.isOptional)
			return false
		return true
	}

	override fun hashCode(): Int {
		var result = baseType.hashCode()
		result = 31 * result + hasDynamicQuantity.hashCode()
		result = 31 * result + isOptional.hashCode()
		return result
	}

	override fun toString(): String {
		var stringRepresentation = ""
		if(hasDynamicQuantity)
			stringRepresentation += "..."
		stringRepresentation += baseType
		if(isOptional)
			stringRepresentation += "?"
		return stringRepresentation
	}
}