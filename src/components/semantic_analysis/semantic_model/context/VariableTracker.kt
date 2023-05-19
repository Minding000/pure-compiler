package components.semantic_analysis.semantic_model.context

import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.issues.initialization.ConstantReassignment
import util.combine
import java.util.*

//TODO also consider value ranges
// - or unions (a is 2 or 10, b is "hi" or "hello")
// - number ranges (a is between 0 and 26, b is lower than zero)
// - other attributes (a is divisible by 2, b is a whole number, etc.)
class VariableTracker(val context: Context, val isInitializer: Boolean = false) {
	val childTrackers = HashMap<String, VariableTracker>()
	val variables = HashMap<ValueDeclaration, MutableList<VariableUsage>>()
	private val ends = HashMap<ValueDeclaration, VariableUsage>()
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
			for((declaration, firstUsages) in variableState.firstVariableUsages)
				getFirstVariableUsagesOf(declaration).addAll(firstUsages)
			for((declaration, lastUsages) in variableState.lastVariableUsages)
				getLastVariableUsagesOf(declaration).addAll(lastUsages)
		}
	}

	fun addLastVariableUsages(lastVariableUsages: Map<ValueDeclaration, Set<VariableUsage>>) {
		for((declaration, usages) in lastVariableUsages) {
			getLastVariableUsagesOf(declaration).addAll(usages)
		}
	}

	private fun getFirstVariableUsagesOf(declaration: ValueDeclaration): MutableSet<VariableUsage> {
		return currentState.firstVariableUsages.getOrPut(declaration) { LinkedHashSet() }
	}

	private fun getLastVariableUsagesOf(declaration: ValueDeclaration): MutableSet<VariableUsage> {
		return currentState.lastVariableUsages.getOrPut(declaration) { LinkedHashSet() }
	}

	fun getCurrentTypeOf(declaration: ValueDeclaration?): Type? {
		if(declaration == null)
			return null
		var type: Type? = null
		for(usage in currentState.lastVariableUsages[declaration] ?: return null) {
			val usageType = usage.resultingType ?: return null
			if(type == null)
				type = usageType
			else if(!type.accepts(usageType))
				type = listOf(type, usageType).combine(declaration)
		}
		return type
	}

	fun getCurrentValueOf(declaration: ValueDeclaration?): Value? {
		var value: Value? = null
		for(usage in currentState.lastVariableUsages[declaration] ?: return null) {
			if(value == null)
				value = usage.resultingValue ?: return null
			else if(value != usage.resultingValue)
				return null
		}
		return value
	}

	fun declare(declaration: ValueDeclaration, isInitialized: Boolean = false) {
		val usages = variables.getOrPut(declaration) { LinkedList() }
		val firstUsages = currentState.firstVariableUsages.getOrPut(declaration) { LinkedHashSet() }
		val types = mutableListOf(VariableUsage.Kind.DECLARATION)
		if(isInitialized || declaration.value != null)
			types.add(VariableUsage.Kind.WRITE)
		val computedType = declaration.value?.getComputedType(this) ?: declaration.type
		val computedValue = declaration.value?.getComputedValue(this)
		val usage = VariableUsage(types, declaration, computedType, computedValue)
		val lastUsages = getLastVariableUsagesOf(declaration)
		usages.add(usage)
		if(firstUsages.isEmpty())
			firstUsages.add(usage)
		lastUsages.clear()
		lastUsages.add(usage)
	}

	fun add(kind: VariableUsage.Kind, variable: VariableValue, resultingType: Type? = getCurrentTypeOf(variable.definition),
			resultingValue: Value? = getCurrentValueOf(variable.definition)): VariableUsage? =
		add(listOf(kind), variable, resultingType, resultingValue)
	fun add(kind: VariableUsage.Kind, declaration: ValueDeclaration, semanticModel: SemanticModel, resultingType: Type? = getCurrentTypeOf(declaration),
			resultingValue: Value? = getCurrentValueOf(declaration)): VariableUsage =
		add(listOf(kind), declaration, semanticModel, resultingType, resultingValue)

	fun add(kinds: List<VariableUsage.Kind>, variable: VariableValue, resultingType: Type? = getCurrentTypeOf(variable.definition),
			resultingValue: Value? = getCurrentValueOf(variable.definition)): VariableUsage? {
		return add(kinds, variable.definition ?: return null, variable, resultingType, resultingValue)
	}

	fun add(kinds: List<VariableUsage.Kind>, declaration: ValueDeclaration, semanticModel: SemanticModel,
			resultingType: Type? = getCurrentTypeOf(declaration),
			resultingValue: Value? = getCurrentValueOf(declaration)): VariableUsage {
		val usages = variables.getOrPut(declaration) { LinkedList() }
		val firstUsages = currentState.firstVariableUsages.getOrPut(declaration) { LinkedHashSet() }
		val lastUsages = getLastVariableUsagesOf(declaration)
		val usage = VariableUsage(kinds, semanticModel, resultingType, resultingValue)
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

	fun calculateEndState() {
		addVariableStates(*returnStatementStates.toTypedArray())
		for((declaration, usages) in currentState.lastVariableUsages) {
			val end = VariableUsage(listOf(VariableUsage.Kind.END), declaration)
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

	fun validate() {
		for((declaration, usages) in variables) {
			for(usage in usages) {
				if(usage.kinds.contains(VariableUsage.Kind.WRITE) && declaration.isConstant) {
					if((declaration is PropertyDeclaration && !isInitializer) || usage.isPreviouslyPossiblyInitialized())
						context.addIssue(ConstantReassignment(usage.semanticModel.source, declaration.name))
				}
			}
		}
	}

	fun getPropertiesBeingInitialized(): List<PropertyDeclaration> {
		val propertiesBeingInitialized = LinkedList<PropertyDeclaration>()
		for((declaration, end) in ends) {
			if(declaration !is PropertyDeclaration)
				continue
			if(end.isPreviouslyInitialized())
				propertiesBeingInitialized.add(declaration)
		}
		return propertiesBeingInitialized
	}

	fun getPropertiesRequiredToBeInitialized(): List<PropertyDeclaration> {
		val propertiesRequiredToBeInitialized = LinkedList<PropertyDeclaration>()
		for((declaration, usages) in variables) {
			if(declaration !is PropertyDeclaration)
				continue
			if(usages.first().isRequiredToBeInitialized())
				propertiesRequiredToBeInitialized.add(declaration)
		}
		return propertiesRequiredToBeInitialized
	}

	fun getReportFor(variableName: String): String? {
		for((declaration, usages) in variables) {
			if(variableName != declaration.name)
				continue
			var report = "start -> ${usages.first().semanticModel.source.start.line.number}"
			for(usage in usages) {
				val typeString = usage.kinds.joinToString(" & ").lowercase()
				var targetString = usage.nextUsages.joinToString()
				if(targetString.isEmpty())
					targetString = "continues raise"
				report += "\n$usage: $typeString -> $targetString (${usage.resultingType}, ${usage.resultingValue})"
			}
			return report
		}
		return null
	}

	class VariableState {
		val firstVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
		val lastVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()

		fun copy(): VariableState {
			val copy = VariableState()
			for((declaration, usages) in firstVariableUsages)
				copy.firstVariableUsages[declaration] = HashSet(usages)
			for((declaration, usages) in lastVariableUsages)
				copy.lastVariableUsages[declaration] = HashSet(usages)
			return copy
		}
	}
}
