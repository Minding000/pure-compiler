package linter.elements.literals

import linter.Linter
import parsing.ast.literals.QuantifiedType as ASTQuantifiedType

class QuantifiedType(val source: ASTQuantifiedType, val baseType: Type, val hasDynamicQuantity: Boolean,
					 val isOptional: Boolean): Type() {

	init {
		units.add(baseType)
		//TODO mind optionality as well
		if(!hasDynamicQuantity)
			scope.addScope(baseType.scope)
	}

	override fun accepts(sourceType: Type): Boolean {
		if(hasDynamicQuantity)
			return false
		if(sourceType is QuantifiedType) {
			if(sourceType.isOptional && !isOptional)
				return false
		}
		return baseType.accepts(sourceType)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		if(hasDynamicQuantity)
			return false
		if(targetType is QuantifiedType) {
			if(isOptional && !targetType.isOptional)
				return false
		}
		return baseType.isAssignableTo(targetType)
	}

	override fun getKeyType(linter: Linter): Type? {
		if(!hasDynamicQuantity)
			return super.getKeyType(linter)
		return SimpleType(source, listOf(), "Int")
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