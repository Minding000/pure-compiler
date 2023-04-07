package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.Element
import java.util.*

class OrUnionType(override val source: Element, scope: Scope, val types: List<Type>): Type(source, scope) {

	init {
		addUnits(types)
		for(type in types)
			type.interfaceScope.subscribe(this)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): OrUnionType {
		val specificTypes = LinkedList<Type>()
		for(type in types)
			specificTypes.add(type.withTypeSubstitutions(linter, typeSubstitutions))
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
		val isOptional = simplifiedUniqueTypes.removeIf { type -> Linter.SpecialType.NULL.matches(type) }
		var simplifiedType = if(simplifiedUniqueTypes.size == 1)
			simplifiedUniqueTypes.first()
		else
			OrUnionType(source, scope, simplifiedUniqueTypes.toList())
		if(isOptional)
			simplifiedType = OptionalType(source, scope, simplifiedType)
		return simplifiedType
	}

	override fun onNewType(type: TypeDefinition) {
		for(part in types)
			if(!part.interfaceScope.hasType(type))
				return
		this.interfaceScope.addType(type)
	}

	override fun onNewValue(value: InterfaceMember) {
		for(part in types)
			if(!part.interfaceScope.hasValue(value))
				return
		this.interfaceScope.addValue(value)
	}

	override fun isInstanceOf(type: Linter.SpecialType): Boolean {
		return types.all { part -> part.isInstanceOf(type) }
	}

	override fun getConversionsFrom(linter: Linter, sourceType: Type): List<InitializerDefinition> {
		return types.flatMap { type -> type.getConversionsFrom(linter, sourceType) }
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = resolveTypeAlias(unresolvedSourceType)
		if(sourceType is OrUnionType)
			return sourceType.isAssignableTo(this)
		for(type in types)
			if(type.accepts(sourceType))
				return true
		return false
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
		for(type in types)
			if(!type.isAssignableTo(targetType))
				return false
		return true
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
