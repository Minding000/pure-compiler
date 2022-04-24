package linter.elements.literals

import linter.Linter
import linter.elements.values.TypeDefinition
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.literals.SimpleType

class SimpleType(val source: SimpleType, val genericTypes: List<Type>, val name: String): Type() {
	var definition: TypeDefinition? = null

	init {
		units.addAll(genericTypes)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		definition = scope.resolveType(name)
		if(definition == null)
			linter.messages.add(Message("Failed to link type '$name' in ${source.getStartString()}.", Message.Type.ERROR))
	}

	override fun accepts(sourceType: Type): Boolean {
		return sourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		if(targetType !is linter.elements.literals.SimpleType)
			return targetType.accepts(this)
		return equals(targetType)
	}

	override fun equals(other: Any?): Boolean {
		if(other !is linter.elements.literals.SimpleType)
			return false
		if(name != other.name)
			return false
		if(genericTypes.size != other.genericTypes.size)
			return false
		for(i in 0..genericTypes.size)
			if(genericTypes[i] == other.genericTypes[i])
				return false
		return true
	}

	override fun hashCode(): Int {
		var result = source.hashCode()
		result = 31 * result + genericTypes.hashCode()
		result = 31 * result + name.hashCode()
		return result
	}
}