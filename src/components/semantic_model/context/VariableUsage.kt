package components.semantic_model.context

import components.semantic_model.general.SemanticModel
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import java.util.*

class VariableUsage(val kinds: List<Kind>, val semanticModel: SemanticModel, var resultingType: Type? = null, var resultingValue: Value? = null) {
	private var isRequiredToBeInitializedCache: Boolean? = null
	private var initializationStateCache = InitializationState.UNKNOWN
	private var isPossiblyInitializedCache: Boolean? = null
	val previousUsages = LinkedList<VariableUsage>()
	val nextUsages = LinkedList<VariableUsage>()
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
		return getInitializationState() == InitializationState.INITIALIZED
	}

	private fun getInitializationState(): InitializationState {
		if(initializationStateCache != InitializationState.UNKNOWN)
			return initializationStateCache
		initializationStateCache = InitializationState.PENDING
		initializationStateCache = if(kinds.contains(Kind.WRITE) || !(previousUsages.isEmpty()
				|| previousUsages.any { usage -> usage.getInitializationState() == InitializationState.UNINITIALIZED}))
			InitializationState.INITIALIZED
		else
			InitializationState.UNINITIALIZED
		return initializationStateCache
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
		var stringRepresentation = semanticModel.source.start.line.number.toString()
//		stringRepresentation += "-"
//		stringRepresentation += semanticModel.source.start.column
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

	enum class InitializationState {
		UNKNOWN,
		PENDING,
		INITIALIZED,
		UNINITIALIZED
	}
}
