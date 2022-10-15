package linting.semantic_model.types

import linting.semantic_model.definitions.OperatorDefinition
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration
import parsing.syntax_tree.literals.UnionType
import java.util.*

class OrUnionType(override val source: UnionType, val types: List<Type>): Type(source) {

	init {
		units.addAll(types)
		for(type in types)
			type.scope.subscribe(this)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): OrUnionType {
		val specificTypes = LinkedList<Type>()
		for(type in types)
			specificTypes.add(typeSubstitution[type] ?: type)
		return OrUnionType(source, specificTypes)
	}

	override fun onNewType(type: TypeDefinition) {
		for(part in types)
			if(!part.scope.hasType(type))
				return
		this.scope.addType(type)
	}

	override fun onNewValue(value: VariableValueDeclaration) {
		for(part in types)
			if(!part.scope.hasValue(value))
				return
		this.scope.addValue(value)
	}

	override fun onNewOperator(operator: OperatorDefinition) {
		for(part in types)
			if(!part.scope.hasOperator(operator))
				return
		this.scope.addOperator(operator)
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = resolveTypeAlias(unresolvedSourceType)
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
		return types.joinToString(" | ")
	}
}
