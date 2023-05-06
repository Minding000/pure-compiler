package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.Element
import java.util.*

class AndUnionType(override val source: Element, scope: Scope, val types: List<Type>): Type(source, scope) {

	init {
		addUnits(types)
		for(type in types)
			type.interfaceScope.subscribe(this)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): AndUnionType {
		val specificTypes = LinkedList<Type>()
		for(type in types)
			specificTypes.add(type.withTypeSubstitutions(linter, typeSubstitutions))
		return AndUnionType(source, scope, specificTypes)
	}

	override fun simplified(): Type {
		if(types.size == 1)
			return types.first().simplified()
		return AndUnionType(source, scope, types.map(Type::simplified))
	}

	override fun onNewType(type: TypeDefinition) {
		interfaceScope.addType(type)
	}

	override fun onNewValue(value: InterfaceMember) {
		interfaceScope.addValue(value)
	}

	override fun onNewInitializer(initializer: InitializerDefinition) {
		interfaceScope.addInitializer(initializer)
	}

	override fun isInstanceOf(type: Linter.SpecialType): Boolean {
		return types.any { part -> part.isInstanceOf(type) }
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = unresolvedSourceType.effectiveType
		for(type in types)
			if(!type.accepts(sourceType))
				return false
		return true
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		if(targetType is AndUnionType)
			targetType.accepts(this)
		for(type in types)
			if(type.isAssignableTo(targetType))
				return true
		return false
	}

	override fun getAbstractMembers(): List<MemberDeclaration> {
		val abstractMembers = LinkedList<MemberDeclaration>()
		for(type in types)
			abstractMembers.addAll(type.getAbstractMembers())
		return abstractMembers
	}

	override fun getPropertiesToBeInitialized(): List<PropertyDeclaration> {
		val propertiesToBeInitialized = LinkedList<PropertyDeclaration>()
		for(type in types)
			propertiesToBeInitialized.addAll(type.getPropertiesToBeInitialized())
		return propertiesToBeInitialized
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
