package linter.elements.literals

import parsing.ast.literals.UnionType

class UnionType(val source: UnionType, val types: List<Type>, val isAnd: Boolean): Type() {

	init {
		units.addAll(types)
	}

	override fun accepts(sourceType: Type): Boolean {
		if(isAnd) {
			for(type in types)
				if(!type.accepts(sourceType))
					return false
			return true
		} else {
			for(type in types)
				if(type.accepts(sourceType))
					return true
			return false
		}
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		if(isAnd) {
			for(type in types)
				if(type.isAssignableTo(targetType))
					return true
			return false
		} else {
			for(type in types)
				if(!type.isAssignableTo(targetType))
					return false
			return true
		}
	}
}