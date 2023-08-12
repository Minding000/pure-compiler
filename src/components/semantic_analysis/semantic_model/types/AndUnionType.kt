package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeDeclaration
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import java.util.*

class AndUnionType(override val source: SyntaxTreeNode, scope: Scope, val types: List<Type>): Type(source, scope) {

	init {
		addSemanticModels(types)
		for(type in types)
			type.interfaceScope.addSubscriber(this)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): AndUnionType {
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

	override fun onNewTypeDeclaration(newTypeDeclaration: TypeDeclaration) {
		interfaceScope.addTypeDeclaration(newTypeDeclaration)
	}

	override fun onNewInterfaceMember(newInterfaceMember: InterfaceMember) {
		interfaceScope.addInterfaceMember(newInterfaceMember)
	}

	override fun onNewInitializer(newInitializer: InitializerDefinition) {
		interfaceScope.addInitializer(newInitializer)
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

	override fun getAbstractMemberDeclarations(): List<MemberDeclaration> {
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
