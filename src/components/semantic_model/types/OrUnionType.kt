package components.semantic_model.types

import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import util.combineOrUnion
import java.util.*

class OrUnionType(override val source: SyntaxTreeNode, scope: Scope, val types: List<Type>): Type(source, scope) {

	init {
		addSemanticModels(types)
	}

	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): OrUnionType {
		val specificTypes = LinkedList<Type>()
		for(type in types)
			specificTypes.add(type.withTypeSubstitutions(typeSubstitutions))
		return OrUnionType(source, scope, specificTypes)
	}

	override fun simplified(): Type {
		return types.combineOrUnion(this)
	}

	override fun getLocalType(value: Value, sourceType: Type): Type {
		return types.map { type -> type.getLocalType(value, sourceType) }.combineOrUnion(this)
	}

	override fun isMemberAccessible(signature: FunctionSignature, requireSpecificType: Boolean): Boolean {
		return types.all { type -> type.isMemberAccessible(signature, requireSpecificType) }
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		var typeDeclaration: TypeDeclaration? = null
		for(type in types)
			typeDeclaration = type.getTypeDeclaration(name) ?: return null
		return typeDeclaration
	}

	override fun getValueDeclaration(name: String): ValueDeclaration.Match? {
		var match: ValueDeclaration.Match? = null
		for(type in types)
			match = type.getValueDeclaration(name) ?: return null
		return match
	}

	override fun isInstanceOf(specialType: SpecialType): Boolean {
		return types.all { part -> part.isInstanceOf(specialType) }
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return types.flatMap { type -> type.getConversionsFrom(sourceType) }
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = unresolvedSourceType.effectiveType
		if(sourceType is OrUnionType)
			return sourceType.isAssignableTo(this)
		return types.any { type -> type.accepts(sourceType) }
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		return types.all { type -> type.isAssignableTo(targetType) }
	}

	override fun equals(other: Any?): Boolean {
		if(other !is OrUnionType)
			return false
		if(types.size != other.types.size)
			return false
		for(type in types)
			if(!other.types.contains(type))
				return false
		return true
	}

	override fun hashCode(): Int {
		return types.hashCode()
	}

	override fun toString(): String {
		return types.sortedBy(Type::toString).joinToString(" | ") { type ->
			if(type is AndUnionType || type is OrUnionType) "($type)" else "$type" }
	}
}
