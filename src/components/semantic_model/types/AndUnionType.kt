package components.semantic_model.types

import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.MemberDeclaration
import components.semantic_model.declarations.PropertyDeclaration
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import java.util.*

class AndUnionType(override val source: SyntaxTreeNode, scope: Scope, val types: List<Type>): Type(source, scope) {

	init {
		addSemanticModels(types)
		for(type in types)
			type.interfaceScope.addSubscriber(this)
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
		return AndUnionType(source, scope, types.map(Type::simplified))
	}

	override fun onNewInitializer(newInitializer: InitializerDefinition) {
		interfaceScope.addInitializer(newInitializer)
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		for(type in types)
			return type.getTypeDeclaration(name) ?: continue
		return null
	}

	override fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?> {
		for(type in types) {
			val valueDeclarationPair = type.getValueDeclaration(name)
			if(valueDeclarationPair.first == null)
				continue
			return valueDeclarationPair
		}
		return Pair(null, null)
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

	override fun getAbstractMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> {
		return types.flatMap { type -> type.getAbstractMemberDeclarations() }
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
