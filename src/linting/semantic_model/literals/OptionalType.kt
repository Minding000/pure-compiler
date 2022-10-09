package linting.semantic_model.literals

import linting.Linter
import parsing.syntax_tree.general.Element

class OptionalType(val source: Element, val baseType: Type): Type() {

	init {
		units.add(baseType)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): OptionalType {
		return OptionalType(source, baseType.withTypeSubstitutions(typeSubstitution))
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = resolveTypeAlias(unresolvedSourceType)
		if(Linter.LiteralType.NULL.matches(sourceType))
			return true
		return baseType.accepts(sourceType)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
		if(targetType !is OptionalType)
			return false
		return baseType.isAssignableTo(targetType.baseType)
	}

	override fun equals(other: Any?): Boolean {
		if(other !is OptionalType)
			return false
		if(baseType != other.baseType)
			return false
		return true
	}

	override fun hashCode(): Int {
		return baseType.hashCode()
	}

	override fun toString(): String {
		return "$baseType?"
	}
}
