package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.Element
import java.util.*

class OrUnionType(override val source: Element, val types: List<Type>): Type(source) {

	init {
		addUnits(types)
		for(type in types)
			type.scope.subscribe(this)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): OrUnionType {
		val specificTypes = LinkedList<Type>()
		for(type in types)
			specificTypes.add(type.withTypeSubstitutions(typeSubstitutions))
		return OrUnionType(source, specificTypes)
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
		return OrUnionType(source, simplifiedUniqueTypes.toList())
	}

	override fun onNewType(type: TypeDefinition) {
		for(part in types)
			if(!part.scope.hasType(type))
				return
		this.scope.addType(type)
	}

	override fun onNewValue(value: InterfaceMember) {
		for(part in types)
			if(!part.scope.hasValue(value))
				return
		this.scope.addValue(value)
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
