package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.declarations.InitializerDefinition
import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

class OptionalType(override val source: SyntaxTreeNode, scope: Scope, val baseType: Type): Type(source, scope) {

	init {
		addSemanticModels(baseType)
	}

	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): OptionalType {
		return OptionalType(source, scope, baseType.withTypeSubstitutions(typeSubstitutions))
	}

	override fun simplified(): Type {
		var baseType = baseType
		while(baseType is OptionalType)
			baseType = baseType.baseType
		return OptionalType(source, scope, baseType.simplified())
	}

	override fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?> {
		return Pair(null, null)
	}

	override fun inferTypeParameter(typeParameter: TypeDeclaration, sourceType: Type, inferredTypes: MutableList<Type>) {
		val sourceBaseType = if(sourceType is OptionalType) sourceType.baseType else sourceType
		baseType.inferTypeParameter(typeParameter, sourceBaseType, inferredTypes)
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return baseType.getConversionsFrom(sourceType)
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		var sourceType = unresolvedSourceType.effectiveType
		if(SpecialType.NULL.matches(sourceType))
			return true
		if(sourceType is OptionalType)
			sourceType = sourceType.baseType
		return baseType.accepts(sourceType)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
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
