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

	override fun linkReferences(linter: Linter, scope: Scope) {
		definition = scope.resolveType(name)
		if(definition == null)
			linter.messages.add(Message("${source.getStartString()}: Failed to link type '$name'.", Message.Type.ERROR))
	}

	override fun accepts(sourceType: Type): Boolean {
		return sourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		if(targetType !is SimpleType)
			return targetType.accepts(this)
		return equals(targetType)
	}

	override fun equals(other: Any?): Boolean {
		if(other !is SimpleType)
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