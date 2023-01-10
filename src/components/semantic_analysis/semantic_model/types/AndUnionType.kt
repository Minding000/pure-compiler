package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.Element
import java.util.*

class AndUnionType(override val source: Element, val types: List<Type>): Type(source) {

	init {
		addUnits(types)
		for(type in types)
			type.scope.subscribe(this)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): AndUnionType {
		val specificTypes = LinkedList<Type>()
		for(type in types)
			specificTypes.add(type.withTypeSubstitutions(typeSubstitutions))
		return AndUnionType(source, specificTypes)
	}

	override fun simplified(): Type {
		if(types.size == 1)
			return types.first().simplified()
		return AndUnionType(source, types.map(Type::simplified))
	}

	override fun onNewType(type: TypeDefinition) {
		this.scope.addType(type)
	}

	override fun onNewValue(value: InterfaceMember) {
		this.scope.addValue(value)
	}

	override fun isInstanceOf(type: Linter.SpecialType): Boolean {
		return types.any { part -> part.isInstanceOf(type) }
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = resolveTypeAlias(unresolvedSourceType)
		for(type in types)
			if(!type.accepts(sourceType))
				return false
		return true
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
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
