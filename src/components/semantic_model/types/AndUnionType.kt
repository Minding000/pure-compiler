package components.semantic_model.types

import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.*
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import java.util.*

class AndUnionType(override val source: SyntaxTreeNode, scope: Scope, val types: List<Type>): Type(source, scope) {

	init {
		addSemanticModels(types)
	}

	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): AndUnionType {
		val specificTypes = LinkedList<Type>()
		for(type in types)
			specificTypes.add(type.withTypeSubstitutions(typeSubstitutions))
		return AndUnionType(source, scope, specificTypes)
	}

	override fun simplified(): Type {
		if(types.size == 1)
			return types.first().simplified()
		//TODO flatten and unions
		//TODO de-duplicate and unions
		return AndUnionType(source, scope, types.map(Type::simplified))
	}

	override fun getLocalType(value: Value, sourceType: Type): Type {
		//TODO call combine function directly instead of creating an intermediate type
		return AndUnionType(source, scope, types.map { type -> type.getLocalType(value, sourceType) }).simplified()
	}

	override fun isMemberAccessible(signature: FunctionSignature, requireSpecificType: Boolean): Boolean {
		return types.any { type -> type.isMemberAccessible(signature, requireSpecificType) }
	}

	override fun getAllInitializers(): List<InitializerDefinition> {
		return types.flatMap { type -> type.getAllInitializers() }
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		for(type in types)
			return type.getTypeDeclaration(name) ?: continue
		return null
	}

	override fun getValueDeclaration(name: String): ValueDeclaration.Match? {
		for(type in types)
			return type.getValueDeclaration(name) ?: continue
		return null
	}

	override fun isInstanceOf(specialType: SpecialType): Boolean {
		return types.any { type -> type.isInstanceOf(specialType) }
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = unresolvedSourceType.effectiveType
		return types.all { type -> type.accepts(sourceType) }
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		if(targetType is AndUnionType)
			targetType.accepts(this)
		return types.any { type -> type.isAssignableTo(targetType) }
	}

	override fun getPotentiallyUnimplementedAbstractMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> {
		return types.flatMap { type -> type.getPotentiallyUnimplementedAbstractMemberDeclarations() }
	}

	override fun implements(abstractMember: MemberDeclaration, typeSubstitutions: Map<TypeDeclaration, Type>): Boolean {
		return types.any { type -> type.implements(abstractMember, typeSubstitutions) }
	}

	override fun getSpecificMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> {
		return types.flatMap { type -> type.getSpecificMemberDeclarations() }
	}

	override fun getPropertiesToBeInitialized(): List<PropertyDeclaration> {
		return types.flatMap { type -> type.getPropertiesToBeInitialized() }
	}

	override fun equals(other: Any?): Boolean {
		if(other !is AndUnionType)
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
		return types.sortedBy(Type::toString).joinToString(" & ") { type ->
			if(type is AndUnionType || type is OrUnionType) "($type)" else "$type" }
	}
}
