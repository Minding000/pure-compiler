package linter.elements.literals

import parsing.ast.literals.UnionType

class OrUnionType(val source: UnionType, val types: List<Type>): Type() {

	init {
		units.addAll(types)
		//TODO init scope to overlap of scopes
	}

	override fun accepts(sourceType: Type): Boolean {
		for(type in types)
			if(type.accepts(sourceType))
				return true
		return false
	}

	override fun isAssignableTo(targetType: Type): Boolean {
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