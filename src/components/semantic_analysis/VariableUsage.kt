package components.semantic_analysis

import components.semantic_analysis.semantic_model.general.Unit
import java.util.*

class VariableUsage(val types: List<Type>, val usage: Unit) {
	var previousUsages = LinkedList<VariableUsage>()
	var nextUsages = LinkedList<VariableUsage>()
	var isFirstUsage = false
	var isLastUsage = false
	var willExit = false

	override fun toString(): String {
		var stringRepresentation = usage.source.start.line.number.toString()
		if(willExit)
			stringRepresentation += "e"
		return stringRepresentation
	}

	enum class Type {
		DECLARATION,
		READ,
		WRITE,
		MUTATION
	}
}
