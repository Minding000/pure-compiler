package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.syntax_parser.syntax_tree.general.Element

class OptionalType(override val source: Element, val baseType: Type): Type(source) {

	init {
		addUnits(baseType)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): OptionalType {
		return OptionalType(source, baseType.withTypeSubstitutions(typeSubstitutions))
	}

	override fun inferType(genericType: TypeDefinition, sourceType: Type, inferredTypes: MutableSet<Type>) {
		val sourceBaseType = if(sourceType is OptionalType)
			sourceType.baseType
		else
			sourceType
		baseType.inferType(genericType, sourceBaseType, inferredTypes)
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		var sourceType = resolveTypeAlias(unresolvedSourceType)
		if(Linter.LiteralType.NULL.matches(sourceType))
			return true
		if(sourceType is OptionalType)
			sourceType = sourceType.baseType
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
