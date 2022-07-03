package linter.elements.literals

import linter.elements.definitions.FunctionDefinition
import parsing.ast.literals.UnionType
import java.util.*

class AndUnionType(val source: UnionType, val types: List<Type>): Type() {

	init {
		units.addAll(types)
		//TODO remove members not shared by all types
		for(originType in types) {
			for((name, type) in originType.scope.types) {
				this.scope.types[name] = type
			}
			for((name, value) in originType.scope.values) {
				this.scope.values[name] = value
			}
			for(initializer in originType.scope.initializers) {
				this.scope.initializers.add(initializer)
			}
			for((name, originalFunctions) in originType.scope.functions) {
				val functions = LinkedList<FunctionDefinition>()
				for(function in originalFunctions)
					functions.add(function)
				this.scope.functions[name] = functions
			}
			for(operator in originType.scope.operators) {
				this.scope.operators.add(operator)
			}
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
		return types.joinToString(" & ")
	}
}