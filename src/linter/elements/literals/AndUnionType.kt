package linter.elements.literals

import parsing.ast.literals.UnionType

class AndUnionType(val source: UnionType, val types: List<Type>): Type() {

	init {
		units.addAll(types)
		for(type in types) {
			//TODO init scope
		}
	}

	override fun accepts(sourceType: Type): Boolean {
		for(type in types)
			if(!type.accepts(sourceType))
				return false
		return true
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		for(type in types)
			if(type.isAssignableTo(targetType))
				return true
		return false
	}
}