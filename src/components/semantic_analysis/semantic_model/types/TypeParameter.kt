package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.definitions.TypeParameter as TypeParameterSyntaxTree

class TypeParameter(override val source: TypeParameterSyntaxTree, scope: Scope, val mode: Mode, val baseType: Type): Type(source, scope) {

	init {
		addSemanticModels(baseType)
	}

	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): TypeParameter {
		return TypeParameter(source, scope, mode, baseType.withTypeSubstitutions(typeSubstitutions))
	}

	override fun simplified(): Type {
		return TypeParameter(source, scope, mode, baseType.simplified())
	}

	override fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?> {
		return baseType.getValueDeclaration(name)
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = unresolvedSourceType.effectiveType
		// If assigning object to collection (different logic applies when assigning a collection)
		if(mode == Mode.PRODUCING)
			return false
		return baseType.accepts(sourceType)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		// If assigning collection to object (different logic applies when assigning to a collection)
		if(mode == Mode.CONSUMING)
			return SpecialType.ANY.matches(targetType)
		return baseType.isAssignableTo(targetType)
	}

	enum class Mode {
		PRODUCING, // effective input type: None
		CONSUMING;  // effective output type: Any

		override fun toString(): String {
			return super.toString().lowercase()
		}
	}

	override fun equals(other: Any?): Boolean {
		if(other !is TypeParameter)
			return false
		if(baseType != other.baseType)
			return false
		if(mode != other.mode)
			return false
		return true
	}

	override fun hashCode(): Int {
		var result = mode.hashCode()
		result = 31 * result + baseType.hashCode()
		return result
	}

	override fun toString(): String {
		return "$baseType $mode"
	}
}
