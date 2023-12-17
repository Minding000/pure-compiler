package components.semantic_model.context

import components.semantic_model.declarations.PropertyDeclaration
import components.semantic_model.general.SemanticModel
import components.semantic_model.types.Type
import components.semantic_model.values.LiteralValue
import components.semantic_model.values.Value
import components.semantic_model.values.ValueDeclaration
import components.semantic_model.values.VariableValue
import logger.issues.initialization.ConstantReassignment
import util.combine
import java.util.*

typealias UsagesByVariable = HashMap<ValueDeclaration, MutableSet<VariableUsage>>

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

	fun addVariableStates(variableStates: Collection<VariableState>) = addVariableStates(*variableStates.toTypedArray())

	fun addVariableStates(vararg variableStates: VariableState) {
		for(variableState in variableStates) {
			for((referencePoint, firstUsages) in variableState.firstVariableUsages) {
				val targetFirstUsages = currentState.firstVariableUsages.getOrPut(referencePoint) { UsagesByVariable() }
				for((declaration, usages) in firstUsages)
					targetFirstUsages.getOrPut(declaration) { LinkedHashSet() }.addAll(usages)
			}
			for((declaration, lastUsages) in variableState.lastVariableUsages)
				getLastVariableUsagesOf(declaration).addAll(lastUsages)
		}
	}

	fun addLastVariableUsages(lastVariableUsages: Map<ValueDeclaration, Set<VariableUsage>>) {
		for((declaration, usages) in lastVariableUsages) {
			getLastVariableUsagesOf(declaration).addAll(usages)
		}
	}

	private fun getLastVariableUsagesOf(declaration: ValueDeclaration): MutableSet<VariableUsage> {
		return currentState.lastVariableUsages.getOrPut(declaration) { LinkedHashSet() }
	}

	fun getCurrentTypeOf(declaration: ValueDeclaration?): Type? {
		return getCommonType(currentState.lastVariableUsages[declaration ?: return null] ?: return declaration.type, declaration)
	}

	private fun getCommonType(usages: Collection<VariableUsage>, semanticModel: SemanticModel): Type? {
		var type: Type? = null
		for(usage in usages) {
			val usageType = usage.resultingType ?: return null
			if(type == null)
				type = usageType
			else if(!type.accepts(usageType))
				type = listOf(type, usageType).combine(semanticModel)
		}
		return type
	}

	fun getCurrentValueOf(declaration: ValueDeclaration?): Value? {
		return getCommonValue(currentState.lastVariableUsages[declaration] ?: return null)
	}

	private fun getCommonValue(usages: Collection<VariableUsage>): Value? {
		var value: Value? = null
		for(usage in usages) {
			if(value == null)
				value = usage.resultingValue ?: return null
			else if(value != usage.resultingValue)
				return null
		}
		return value
	}

	fun declare(declaration: ValueDeclaration, isInitialized: Boolean = false) {
		val usages = variables.getOrPut(declaration) { LinkedList() }
		val types = mutableListOf(VariableUsage.Kind.DECLARATION)
		if(isInitialized || declaration.value != null)
			types.add(VariableUsage.Kind.WRITE)
		val computedType = declaration.value?.getComputedType() ?: declaration.type
		val computedValue = declaration.value?.getComputedValue()
		val usage = VariableUsage(types, declaration, computedType, computedValue)
		val lastUsages = getLastVariableUsagesOf(declaration)
		usages.add(usage)
		for((_, firstVariableUsages) in currentState.firstVariableUsages) {
			val firstUsages = firstVariableUsages.getOrPut(declaration) { LinkedHashSet() }
			if(firstUsages.isEmpty())
				firstUsages.add(usage)
		}
		lastUsages.clear()
		lastUsages.add(usage)
	}

	fun add(kind: VariableUsage.Kind, variable: VariableValue, resultingType: Type? = getCurrentTypeOf(variable.declaration),
			resultingValue: Value? = getCurrentValueOf(variable.declaration)): VariableUsage? =
		add(listOf(kind), variable, resultingType, resultingValue)
	fun add(kind: VariableUsage.Kind, declaration: ValueDeclaration, semanticModel: SemanticModel, resultingType: Type? = getCurrentTypeOf(declaration),
			resultingValue: Value? = getCurrentValueOf(declaration)): VariableUsage =
		add(listOf(kind), declaration, semanticModel, resultingType, resultingValue)

	fun add(kinds: List<VariableUsage.Kind>, variable: VariableValue, resultingType: Type? = getCurrentTypeOf(variable.declaration),
			resultingValue: Value? = getCurrentValueOf(variable.declaration)): VariableUsage? {
		return add(kinds, variable.declaration ?: return null, variable, resultingType, resultingValue)
	}

	fun add(kinds: List<VariableUsage.Kind>, declaration: ValueDeclaration, semanticModel: SemanticModel,
			resultingType: Type? = getCurrentTypeOf(declaration),
			resultingValue: Value? = getCurrentValueOf(declaration)): VariableUsage {
		val usages = variables.getOrPut(declaration) { LinkedList() }
		val lastUsages = getLastVariableUsagesOf(declaration)
		val usage = VariableUsage(kinds, semanticModel, resultingType, resultingValue)
		for(lastUsage in lastUsages) {
			usage.previousUsages.add(lastUsage)
			lastUsage.nextUsages.add(usage)
		}
		usages.add(usage)
		for((_, firstVariableUsages) in currentState.firstVariableUsages) {
			val firstUsages = firstVariableUsages.getOrPut(declaration) { LinkedHashSet() }
			if(firstUsages.isEmpty())
				firstUsages.add(usage)
		}
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

	fun linkBackTo(referencePoint: VariableState.ReferencePoint) {
		link(currentState, referencePoint)
	}

	fun link(originState: VariableState, referencePoint: VariableState.ReferencePoint) {
		val firstVariableUsages = currentState.firstVariableUsages[referencePoint] ?: return
		for((declaration, lastUsages) in originState.lastVariableUsages) {
			for(firstUsage in firstVariableUsages[declaration] ?: continue) {
				if(firstUsage.kinds.contains(VariableUsage.Kind.DECLARATION)) {
					val end = VariableUsage(listOf(VariableUsage.Kind.END), declaration)
					for(lastUsage in lastUsages)
						lastUsage.nextUsages.add(end)
					continue
				}
				for(lastUsage in lastUsages) {
					firstUsage.previousUsages.addFirst(lastUsage)
					lastUsage.nextUsages.addFirst(firstUsage)
				}
			}
		}
	}

	fun calculateEndState() {
		addVariableStates(returnStatementStates)
		for((declaration, usages) in currentState.lastVariableUsages) {
			val end = VariableUsage(listOf(VariableUsage.Kind.END), declaration)
			ends[declaration] = end
			end.previousUsages.addAll(usages)
			for(usage in usages)
				usage.nextUsages.add(end)
		}
	}

	fun markAllUsagesAsExiting(referencePoint: VariableState.ReferencePoint) {
		var currentUsages = currentState.firstVariableUsages[referencePoint] ?: return
		while(true) {
			var hasNextUsages = false
			val nextVariableUsages = UsagesByVariable()
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

	fun collectAllUsagesInto(referencePoint: VariableState.ReferencePoint, variableUsages: UsagesByVariable) {
		var currentUsages = currentState.firstVariableUsages[referencePoint] ?: return
		while(true) {
			var hasNextUsages = false
			val nextVariableUsages = UsagesByVariable()
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
				val value = usage.resultingValue
				val valueStringRepresentation = if(value is LiteralValue || value == null)
					value.toString()
				else
					"Expression"
				report += "\n$usage: $typeString -> $targetString (${usage.resultingType}, $valueStringRepresentation)"
			}
			return report
		}
		return null
	}

	class VariableState {
		val firstVariableUsages = HashMap<ReferencePoint, UsagesByVariable>()
		val lastVariableUsages = UsagesByVariable()

		fun copy(): VariableState {
			val copy = VariableState()
			for((referencePoint, firstUsages) in firstVariableUsages) {
				val firstUsagesCopy = UsagesByVariable()
				for((declaration, usages) in firstUsages)
					firstUsagesCopy[declaration] = HashSet(usages)
				copy.firstVariableUsages[referencePoint] = firstUsagesCopy
			}
			for((declaration, usages) in lastVariableUsages)
				copy.lastVariableUsages[declaration] = HashSet(usages)
			return copy
		}

		fun createReferencePoint(): ReferencePoint {
			val referencePoint = ReferencePoint()
			firstVariableUsages[referencePoint] = UsagesByVariable()
			return referencePoint
		}

		fun removeReferencePoint(referencePoint: ReferencePoint) {
			firstVariableUsages.remove(referencePoint)
		}

		class ReferencePoint
	}
}
