package linter.elements.literals

import linter.Linter
import linter.messages.Message
import parsing.ast.literals.QuantifiedType

class QuantifiedType(val source: QuantifiedType, val baseType: Type, val hasDynamicQuantity: Boolean,
					 val isOptional: Boolean): Type() {

	init {
		units.add(baseType)
	}

	override fun accepts(sourceType: Type): Boolean {
		if(hasDynamicQuantity)
			return false
		if(sourceType is linter.elements.literals.QuantifiedType) {
			if(sourceType.isOptional && !isOptional)
				return false
		}
		return baseType.accepts(sourceType)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		if(hasDynamicQuantity)
			return false
		if(targetType is linter.elements.literals.QuantifiedType) {
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