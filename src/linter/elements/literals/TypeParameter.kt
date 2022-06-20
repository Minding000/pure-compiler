package linter.elements.literals

import parsing.ast.definitions.TypeParameter as ASTTypeParameter

class TypeParameter(val source: ASTTypeParameter, val mode: Mode, val baseType: Type): Type() {

	init {
		units.add(baseType)
	}

	override fun accepts(sourceType: Type): Boolean {
		// If assigning object to collection (different logic applies when assigning a collection)
		if(mode == Mode.PRODUCING)
			return false
		return baseType.accepts(sourceType)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		// If assigning collection to object (different logic applies when assigning to a collection)
		if(mode == Mode.CONSUMING)
			return false
		return baseType.isAssignableTo(targetType)
	}

	enum class Mode {
		PRODUCING, // effective input type: None
		CONSUMING  // effective output type: Any
	}

	override fun equals(other: Any?): Boolean {
		if(other !is TypeParameter)
			return false
		if(baseType != other.baseType)
			return false
		if(mode != other.mode)
			return false
		return true
	}

	override fun hashCode(): Int {
		var result = mode.hashCode()
		result = 31 * result + baseType.hashCode()
		return result
	}

	override fun toString(): String {
		return "$mode $baseType"
	}
}