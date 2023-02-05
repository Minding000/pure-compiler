package components.semantic_analysis

import components.semantic_analysis.semantic_model.general.Unit
import java.util.*

class VariableUsage(val types: List<Type>, val usage: Unit) {
	var previousUsages = LinkedList<VariableUsage>()
	var nextUsages = LinkedList<VariableUsage>()
	var isFirstUsage = false
	var isLastUsage = false
	var willExit = false
	private var isInitializedCache: Boolean? = null
	private var isPossiblyInitializedCache: Boolean? = null

	fun isPreviouslyInitialized(): Boolean {
		if(previousUsages.isEmpty())
			return false
		if(previousUsages.all(VariableUsage::isInitialized))
			return true
		return false
	}

	private fun isInitialized(): Boolean {
		var isInitialized = isInitializedCache
		if(isInitialized != null)
			return isInitialized
		isInitializedCache = false
		isInitialized = types.contains(Type.WRITE) || (previousUsages.isNotEmpty() && previousUsages.all(VariableUsage::isInitialized))
		isInitializedCache = isInitialized
		return isInitialized
	}

	fun isPreviouslyPossiblyInitialized(): Boolean {
		if(previousUsages.isEmpty())
			return false
		if(previousUsages.any(VariableUsage::isPossiblyInitialized))
			return true
		return false
	}

	private fun isPossiblyInitialized(): Boolean {
		var isPossiblyInitialized = isPossiblyInitializedCache
		if(isPossiblyInitialized != null)
			return isPossiblyInitialized
		isPossiblyInitializedCache = false
		isPossiblyInitialized = types.contains(Type.WRITE) || (previousUsages.isNotEmpty() && previousUsages.any(VariableUsage::isPossiblyInitialized))
		isPossiblyInitializedCache = isPossiblyInitialized
		return isPossiblyInitialized
	}

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
