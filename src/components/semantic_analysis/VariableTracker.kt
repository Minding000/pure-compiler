package components.semantic_analysis

import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue
import java.util.*

class VariableTracker(val isInitializer: Boolean = false) {
	val childTrackers = HashMap<String, VariableTracker>()
	val variables = HashMap<ValueDeclaration, MutableList<VariableUsage>>()
	val ends = HashMap<ValueDeclaration, VariableUsage>()
	val currentState = VariableState()
	val nextStatementStates = LinkedList<VariableState>()
	val breakStatementStates = LinkedList<VariableState>()
	private val returnStatementStates = LinkedList<VariableState>()

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

	private fun getLastVariableUsages(declaration: ValueDeclaration): MutableSet<VariableUsage> {
		return currentState.lastVariableUsages.getOrPut(declaration) { LinkedHashSet<VariableUsage>() }
	}

	fun declare(declaration: ValueDeclaration, isInitialized: Boolean = false) {
		val usages = variables.getOrPut(declaration) { LinkedList() }
		val firstUsages = currentState.firstVariableUsages.getOrPut(declaration) { LinkedHashSet() }
		val types = mutableListOf(VariableUsage.Type.DECLARATION)
		if(isInitialized || declaration.value != null)
			types.add(VariableUsage.Type.WRITE)
		val usage = VariableUsage(types, declaration)
		val lastUsages = getLastVariableUsages(declaration)
		usages.add(usage)
		if(firstUsages.isEmpty())
			firstUsages.add(usage)
		lastUsages.clear()
		lastUsages.add(usage)
	}

	fun add(type: VariableUsage.Type, variable: VariableValue): VariableUsage? = add(listOf(type), variable)
	fun add(type: VariableUsage.Type, declaration: ValueDeclaration, unit: Unit): VariableUsage = add(listOf(type), declaration, unit)

	fun add(types: List<VariableUsage.Type>, variable: VariableValue): VariableUsage? {
		return add(types, variable.definition ?: return null, variable)
	}

	fun add(types: List<VariableUsage.Type>, declaration: ValueDeclaration, unit: Unit): VariableUsage {
		val usages = variables.getOrPut(declaration) { LinkedList() }
		val firstUsages = currentState.firstVariableUsages.getOrPut(declaration) { LinkedHashSet() }
		val usage = VariableUsage(types, unit)
		val lastUsages = getLastVariableUsages(declaration)
		for(lastUsage in lastUsages) {
			usage.previousUsages.add(lastUsage)
			lastUsage.nextUsages.add(usage)
		}
		usages.add(usage)
		if(firstUsages.isEmpty())
			firstUsages.add(usage)
		lastUsages.clear()
		lastUsages.add(usage)
		return usage
	}

	fun registerNextStatement() {
		nextStatementStates.add(currentState.copy())
		currentState.lastVariableUsages.clear()
	}

	fun registerBreakStatement() {
		breakStatementStates.add(currentState.copy())
		currentState.lastVariableUsages.clear()
	}

	fun registerReturnStatement() {
		returnStatementStates.add(currentState.copy())
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
		addVariableStates(*returnStatementStates.toTypedArray())
		for((declaration, usages) in currentState.lastVariableUsages) {
			val end = VariableUsage(listOf(VariableUsage.Type.END), declaration)
			ends[declaration] = end
			end.previousUsages.addAll(usages)
			for(usage in usages)
				usage.nextUsages.add(end)
		}
	}

	fun markAllUsagesAsExiting() {
		var currentUsages = currentState.firstVariableUsages
		while(true) {
			var hasNextUsages = false
			val nextVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
			usagesToBeLinked@for((declaration, usages) in currentUsages) {
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
			var report = "start -> ${usages.first().unit.source.start.line.number}"
			for(usage in usages) {
				val typeString = usage.types.joinToString(" & ").lowercase()
				var targetString = usage.nextUsages.joinToString()
				if(targetString.isEmpty())
					targetString = "continues raise"
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
