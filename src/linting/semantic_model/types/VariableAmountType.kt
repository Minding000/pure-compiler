package linting.semantic_model.types

import linting.Linter
import linting.semantic_model.definitions.TypeDefinition
import parsing.syntax_tree.general.Element

class VariableAmountType(override val source: Element, val baseType: Type): Type(source) {

	init {
		units.add(baseType)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): VariableAmountType {
		return VariableAmountType(source, baseType.withTypeSubstitutions(typeSubstitutions))
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = resolveTypeAlias(unresolvedSourceType)
		if(sourceType !is VariableAmountType)
			return false
		return baseType.accepts(sourceType.baseType)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
		if(targetType !is VariableAmountType)
			return false
		return baseType.isAssignableTo(targetType.baseType)
	}

	override fun getKeyType(linter: Linter): Type {
		return ObjectType(source, "Int")
	}

	override fun getValueType(linter: Linter): Type {
		return baseType
	}

	override fun equals(other: Any?): Boolean {
		if(other !is VariableAmountType)
			return false
		if(baseType != other.baseType)
			return false
		return true
	}

	override fun hashCode(): Int {
		return baseType.hashCode()
	}

	override fun toString(): String {
		return "...$baseType"
	}
}
