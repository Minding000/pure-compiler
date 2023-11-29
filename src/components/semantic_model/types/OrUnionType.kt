package components.semantic_model.types

import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
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
		val simplifiedFlattenedTypes = LinkedList<Type>()
		for(type in types) {
			val simplifiedType = type.simplified()
			if(simplifiedType is OrUnionType) {
				for(part in simplifiedType.types)
					simplifiedFlattenedTypes.add(part)
				continue
			}
			simplifiedFlattenedTypes.add(type)
		}
		val simplifiedUniqueTypes = HashSet<Type>()
		uniqueTypeSearch@for(type in simplifiedFlattenedTypes) {
			for(otherType in simplifiedFlattenedTypes) {
				if(otherType == type)
					continue
				if(otherType.accepts(type))
					continue@uniqueTypeSearch
			}
			simplifiedUniqueTypes.add(type)
		}
		if(simplifiedUniqueTypes.size == 1)
			return simplifiedUniqueTypes.first()
		val isOptional = simplifiedUniqueTypes.removeIf { type -> SpecialType.NULL.matches(type) }
		var simplifiedType = if(simplifiedUniqueTypes.size == 1)
			simplifiedUniqueTypes.first()
		else
			OrUnionType(source, scope, simplifiedUniqueTypes.toList())
		if(isOptional)
			simplifiedType = OptionalType(source, scope, simplifiedType)
		return simplifiedType
	}

	override fun getLocalType(value: Value, sourceType: Type): Type {
		return OrUnionType(source, scope, types.map { type -> type.getLocalType(value, sourceType) }).simplified()
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

	override fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?> {
		var valueDeclarationPair: Pair<ValueDeclaration?, Type?>? = null
		for(type in types) {
			valueDeclarationPair = type.getValueDeclaration(name)
			if(valueDeclarationPair.first == null)
				return Pair(null, null)
		}
		return valueDeclarationPair ?: Pair(null, null)
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
