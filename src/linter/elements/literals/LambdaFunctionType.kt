package linter.elements.literals

import parsing.ast.definitions.LambdaFunctionType as ASTLambdaType

class LambdaFunctionType(val source: ASTLambdaType, val parameters: List<Type>, val returnType: Type?): Type() {

	init {
		units.addAll(parameters)
		if(returnType != null)
			units.add(returnType)
	}

	override fun accepts(sourceType: Type): Boolean {
		return sourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		if(targetType !is LambdaFunctionType)
			return targetType.accepts(this)
		return equals(targetType)
	}

	override fun equals(other: Any?): Boolean {
		if(other !is LambdaFunctionType)
			return false
		if(returnType != other.returnType)
			return false
		if(parameters.size != other.parameters.size)
			return false
		for(i in 0..parameters.size)
			if(parameters[i] != other.parameters[i])
				return false
		return true
	}

	override fun hashCode(): Int {
		var result = source.hashCode()
		result = 31 * result + parameters.hashCode()
		result = 31 * result + (returnType?.hashCode() ?: 0)
		return result
	}
}