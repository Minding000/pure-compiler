package linter.elements.literals

import linter.Linter
import linter.elements.values.TypeDefinition
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.general.Element
import java.util.LinkedList

class SimpleType(val source: Element, val genericTypes: List<Type>, val name: String): Type() {
	var definition: TypeDefinition? = null

	constructor(definition: TypeDefinition): this(definition.source, LinkedList(), definition.name) {
		this.definition = definition
	}

	init {
		units.addAll(genericTypes)
		if(definition != null) {
			//TODO init scope
		}
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		definition = scope.resolveType(name)
		if(definition == null)
			linter.messages.add(Message("${source.getStartString()}: Type '$name' hasn't been declared yet.", Message.Type.ERROR))
	}

	override fun accepts(sourceType: Type): Boolean {
		return sourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		if(targetType !is SimpleType)
			return targetType.accepts(this)
		return equals(targetType)
	}

	override fun getKeyType(linter: Linter): Type? {
		if(genericTypes.size != 2) {
			linter.messages.add(Message("Type '$this' doesn't have a key type.", Message.Type.ERROR))
			return null
		}
		return genericTypes.first()
	}

	override fun getValueType(linter: Linter): Type? {
		if(!(genericTypes.size == 1 || genericTypes.size == 2)) {
			linter.messages.add(Message("Type '$this' doesn't have a value type.", Message.Type.ERROR))
			return null
		}
		return genericTypes.last()
	}

	override fun equals(other: Any?): Boolean {
		if(other !is SimpleType)
			return false
		if(definition != other.definition)
			return false
		if(genericTypes.size != other.genericTypes.size)
			return false
		for(i in genericTypes.indices)
			if(genericTypes[i] == other.genericTypes[i])
				return false
		return true
	}

	override fun hashCode(): Int {
		var result = genericTypes.hashCode()
		result = 31 * result + (definition?.hashCode() ?: 0)
		return result
	}

	override fun toString(): String {
		return name
	}
}