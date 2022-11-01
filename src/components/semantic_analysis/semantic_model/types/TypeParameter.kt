package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.syntax_parser.syntax_tree.definitions.TypeParameter as TypeParameterSyntaxTree

class TypeParameter(override val source: TypeParameterSyntaxTree, val mode: Mode, val baseType: Type): Type(source) {

	init {
		units.add(baseType)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): TypeParameter {
		return TypeParameter(source, mode, baseType.withTypeSubstitutions(typeSubstitutions))
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = resolveTypeAlias(unresolvedSourceType)
		// If assigning object to collection (different logic applies when assigning a collection)
		if(mode == Mode.PRODUCING)
			return false
		return baseType.accepts(sourceType)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
		// If assigning collection to object (different logic applies when assigning to a collection)
		if(mode == Mode.CONSUMING)
			return Linter.LiteralType.ANY.matches(targetType)
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
