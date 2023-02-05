package components.semantic_analysis

import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue
import java.util.*

class VariableTracker(val isInitializer: Boolean = false) {
	val childTrackers = HashMap<String, VariableTracker>()
	val variables = HashMap<ValueDeclaration, MutableList<VariableUsage>>()
	val currentState = VariableState()
	val nextStatementStates = LinkedList<VariableState>()
	val breakStatementStates = LinkedList<VariableState>()

	fun setVariableStates(vararg variableStates: VariableState) {
		currentState.lastVariableUsages.clear()
		addVariableStates(*variableStates)
	}

	fun addVariableStates(vararg variableStates: VariableState) {
		for(variableState in variableStates) {
			for((declaration, firstUsages) in variableState.firstVariableUsages) {
				val allFirstUsages = currentState.firstVariableUsages.getOrPut(declaration) { LinkedHashSet() }
				allFirstUsages.addAll(firstUsages)
			}
			for((declaration, lastUsages) in variableState.lastVariableUsages) {
				val allLastUsages = currentState.lastVariableUsages.getOrPut(declaration) { LinkedHashSet() }
				allLastUsages.addAll(lastUsages)
			}
		}
	}

	fun declare(declaration: ValueDeclaration) {
		val usages = variables.getOrPut(declaration) { LinkedList() }
		val firstUsages = currentState.firstVariableUsages.getOrPut(declaration) { LinkedHashSet() }
		val previousUsages = currentState.lastVariableUsages.getOrPut(declaration) { LinkedHashSet() }
		val types = mutableListOf(VariableUsage.Type.DECLARATION)
		if(declaration.value != null)
			types.add(VariableUsage.Type.WRITE)
		val usage = VariableUsage(types, declaration)
		usages.add(usage)
		if(firstUsages.isEmpty())
			firstUsages.add(usage)
		previousUsages.clear()
		previousUsages.add(usage)
	}

	fun add(type: VariableUsage.Type, variable: VariableValue): VariableUsage? = add(listOf(type), variable)

	fun add(types: List<VariableUsage.Type>, variable: VariableValue): VariableUsage? {
		val usages = variables.getOrPut(variable.definition ?: return null) { LinkedList() }
		val firstUsages = currentState.firstVariableUsages.getOrPut(variable.definition ?: return null) { LinkedHashSet() }
		val lastUsages = currentState.lastVariableUsages.getOrPut(variable.definition ?: return null) { LinkedHashSet() }
		val usage = VariableUsage(types, variable)
		for(previousUsage in lastUsages) {
			usage.previousUsages.add(previousUsage)
			previousUsage.nextUsages.add(usage)
		}
		usages.add(usage)
		if(firstUsages.isEmpty())
			firstUsages.add(usage)
		lastUsages.clear()
		lastUsages.add(usage)
		return usage
	}

	fun registerExecutionEnd() {
		for((_, usages) in currentState.lastVariableUsages) {
			for(usage in usages) {
				usage.isLastUsage = true
			}
		}
		currentState.lastVariableUsages.clear()
	}

	fun registerNextStatement() {
		nextStatementStates.add(currentState.copy())
		currentState.lastVariableUsages.clear()
	}

	fun registerBreakStatement() {
		breakStatementStates.add(currentState.copy())
		currentState.lastVariableUsages.clear()
	}

	fun linkBackToStart() {
		link(currentState, currentState)
	}

	fun linkBackToStartFrom(referenceState: VariableState) {
		link(referenceState, currentState)
	}

	fun link(originState: VariableState, targetState: VariableState) {
		for((declaration, lastUsages) in originState.lastVariableUsages) {
			for(firstUsage in targetState.firstVariableUsages[declaration] ?: continue) {
				for(lastUsage in lastUsages) {
					firstUsage.previousUsages.addFirst(lastUsage)
					lastUsage.nextUsages.addFirst(firstUsage)
				}
			}
		}
	}

	fun linkToStartFrom(variableUsages: HashMap<ValueDeclaration, MutableSet<VariableUsage>>) {
		for((declaration, usages) in variableUsages) {
			for(firstUsage in currentState.firstVariableUsages[declaration] ?: continue) {
				for(usage in usages) {
					usage.nextUsages.add(firstUsage)
					firstUsage.previousUsages.add(usage)
				}
			}
		}
	}

	fun calculateEndState() {
		registerExecutionEnd()
		for((_, usages) in variables) {
			for(usage in usages) {
				if(usage.previousUsages.isEmpty())
					usage.isFirstUsage = true
			}
		}
	}

	fun markAllUsagesAsExiting() {
		var currentUsages = currentState.firstVariableUsages
		while(true) {
			var hasNextUsages = false
			val nextVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
			usagesToBeLinked@ for((declaration, usages) in currentUsages) {
				val nextUsages = nextVariableUsages.getOrPut(declaration) { HashSet() }
				for(usage in usages) {
					usage.willExit = true
					hasNextUsages = true
					nextUsages.addAll(usage.nextUsages)
				}
			}
			if(!hasNextUsages)
				break
			currentUsages = nextVariableUsages
		}
	}

	fun collectAllUsagesInto(variableUsages: HashMap<ValueDeclaration, MutableSet<VariableUsage>>) {
		var currentUsages = currentState.firstVariableUsages
		while(true) {
			var hasNextUsages = false
			val nextVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
			for((declaration, usages) in currentUsages) {
				val cumulatedUsages = variableUsages.getOrPut(declaration) { HashSet() }
				cumulatedUsages.addAll(usages)
				val nextUsages = nextVariableUsages.getOrPut(declaration) { HashSet() }
				for(usage in usages) {
					hasNextUsages = true
					nextUsages.addAll(usage.nextUsages)
				}
			}
			if(!hasNextUsages)
				break
			currentUsages = nextVariableUsages
		}
	}

	fun addChild(name: String, fileTracker: VariableTracker) {
		childTrackers[name] = fileTracker
	}

	fun getReport(variableName: String): String? {
		for((declaration, usages) in variables) {
			if(variableName != declaration.name)
				continue
			var report = "start -> ${usages.filter(VariableUsage::isFirstUsage).joinToString()}"
			for(usage in usages) {
				val typeString = usage.types.joinToString(" & ").lowercase()
				var targetString = usage.nextUsages.joinToString()
				if(usage.isLastUsage) {
					if(targetString.isNotEmpty())
						targetString += ", "
					targetString += "end"
				}
				report += "\n$usage: $typeString -> $targetString"
			}
			return report
		}
		return null
	}

	class VariableState {
		val firstVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
		val lastVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()

		fun copy(): VariableState {
			val newState = VariableState()
			for((declaration, usages) in firstVariableUsages)
				newState.firstVariableUsages[declaration] = HashSet(usages)
			for((declaration, usages) in lastVariableUsages)
				newState.lastVariableUsages[declaration] = HashSet(usages)
			return newState
		}
	}
}
