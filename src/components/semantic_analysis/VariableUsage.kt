package components.semantic_analysis

import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LiteralValue
import java.util.*

class VariableUsage(val kinds: List<Kind>, val unit: Unit, var resultingType: Type? = null,
					var resultingLiteralValue: LiteralValue? = null) {
	private var isRequiredToBeInitializedCache: Boolean? = null
	private var isInitializedCache: Boolean? = null
	private var isPossiblyInitializedCache: Boolean? = null
	var previousUsages = LinkedList<VariableUsage>()
	var nextUsages = LinkedList<VariableUsage>()
	var willExit = false

	fun isRequiredToBeInitialized(): Boolean {
		var isRequiredToBeInitialized = isRequiredToBeInitializedCache
		if(isRequiredToBeInitialized != null)
			return isRequiredToBeInitialized
		isRequiredToBeInitializedCache = false
		isRequiredToBeInitialized = kinds.contains(Kind.READ)
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
		isInitialized = kinds.contains(Kind.WRITE) || (previousUsages.isNotEmpty() && previousUsages.all(VariableUsage::isNowInitialized))
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
		isPossiblyInitialized = kinds.contains(Kind.WRITE)
			|| (previousUsages.isNotEmpty() && previousUsages.any(VariableUsage::isNowPossiblyInitialized))
		isPossiblyInitializedCache = isPossiblyInitialized
		return isPossiblyInitialized
	}

	override fun toString(): String {
		if(kinds.contains(Kind.END))
			return "end"
		var stringRepresentation = unit.source.start.line.number.toString()
//		stringRepresentation += "-"
//		stringRepresentation += unit.source.start.column
		if(willExit)
			stringRepresentation += "e"
		return stringRepresentation
	}

	enum class Kind {
		DECLARATION,
		READ,
		WRITE,
		MUTATION,
		HINT,
		END
	}
}
