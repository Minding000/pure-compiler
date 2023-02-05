package components.semantic_analysis

import components.semantic_analysis.semantic_model.general.Unit
import java.util.*

class VariableUsage(val types: List<Type>, val unit: Unit) {
	var previousUsages = LinkedList<VariableUsage>()
	var nextUsages = LinkedList<VariableUsage>()
	var willExit = false
	private var isRequiredToBeInitializedCache: Boolean? = null
	private var isInitializedCache: Boolean? = null
	private var isPossiblyInitializedCache: Boolean? = null

	fun isRequiredToBeInitialized(): Boolean {
		var isRequiredToBeInitialized = isRequiredToBeInitializedCache
		if(isRequiredToBeInitialized != null)
			return isRequiredToBeInitialized
		isRequiredToBeInitializedCache = false
		isRequiredToBeInitialized = types.contains(Type.READ)
			|| (nextUsages.isNotEmpty() && nextUsages.any(VariableUsage::isRequiredToBeInitialized))
		isRequiredToBeInitializedCache = isRequiredToBeInitialized
		return isRequiredToBeInitialized
	}

	fun isPreviouslyInitialized(): Boolean {
		if(previousUsages.isEmpty())
			return false
		if(previousUsages.all(VariableUsage::isNowInitialized))
			return true
		return false
	}

	fun isNowInitialized(): Boolean {
		var isInitialized = isInitializedCache
		if(isInitialized != null)
			return isInitialized
		isInitializedCache = false
		isInitialized = types.contains(Type.WRITE) || (previousUsages.isNotEmpty() && previousUsages.all(VariableUsage::isNowInitialized))
		isInitializedCache = isInitialized
		return isInitialized
	}

	fun isPreviouslyPossiblyInitialized(): Boolean {
		if(previousUsages.isEmpty())
			return false
		if(previousUsages.any(VariableUsage::isNowPossiblyInitialized))
			return true
		return false
	}

	fun isNowPossiblyInitialized(): Boolean {
		var isPossiblyInitialized = isPossiblyInitializedCache
		if(isPossiblyInitialized != null)
			return isPossiblyInitialized
		isPossiblyInitializedCache = false
		isPossiblyInitialized = types.contains(Type.WRITE)
			|| (previousUsages.isNotEmpty() && previousUsages.any(VariableUsage::isNowPossiblyInitialized))
		isPossiblyInitializedCache = isPossiblyInitialized
		return isPossiblyInitialized
	}

	override fun toString(): String {
		if(types.contains(Type.END))
			return "end"
		var stringRepresentation = unit.source.start.line.number.toString()
		if(willExit)
			stringRepresentation += "e"
		return stringRepresentation
	}

	enum class Type {
		DECLARATION,
		READ,
		WRITE,
		MUTATION,
		END
	}
}
