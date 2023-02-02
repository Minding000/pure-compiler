package components.semantic_analysis

import components.semantic_analysis.semantic_model.control_flow.*
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.*
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.operations.Assignment
import components.semantic_analysis.semantic_model.operations.BinaryModification
import components.semantic_analysis.semantic_model.operations.BinaryOperator
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue
import java.util.*

object DataFlowAnalyser {

	fun analyseDataFlow(program: Program): VariableTracker {
		val tracker = VariableTracker()
		for(file in program.files)
			analyseDataFlow(file, tracker)
		tracker.calculateEndState()
		return tracker
	}

	private fun analyseDataFlow(unit: Unit, tracker: VariableTracker) {
		when(unit) {
			is File -> {
				val fileTracker = VariableTracker()
				for(unitInFile in unit.units)
					analyseDataFlow(unitInFile, fileTracker)
				fileTracker.calculateEndState()
				tracker.addChild(unit.file.name, fileTracker)
			}
			is TypeDefinition -> {
				for(member in unit.scope.memberDeclarations) {
					if(member is FunctionImplementation)
						analyseDataFlow(member, tracker)
				}
			}
			is FunctionImplementation -> {
				val body = unit.body ?: return
				val functionTracker = VariableTracker()
				analyseDataFlow(body, functionTracker)
				functionTracker.calculateEndState()
				tracker.addChild(unit.parentFunction.name, functionTracker)
			}
			is ErrorHandlingContext -> {
				val initialState = tracker.currentState.copy()
				// Analyse main block
				tracker.currentState.firstVariableUsages.clear()
				analyseDataFlow(unit.mainBlock, tracker)
				val mainBlockState = tracker.currentState.copy()
				// Collect usages that should link to the handle blocks
				val potentiallyLastVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
				tracker.collectAllUsagesInto(potentiallyLastVariableUsages)
				val handleBlockStates = LinkedList<VariableTracker.VariableState>()
				if(unit.handleBlocks.isNotEmpty()) {
					// Analyse handle blocks
					for(handleBlock in unit.handleBlocks) {
						tracker.setVariableStates(initialState)
						tracker.currentState.firstVariableUsages.clear()
						analyseDataFlow(handleBlock, tracker)
						tracker.linkToFirstUsages(potentiallyLastVariableUsages)
						tracker.collectAllUsagesInto(potentiallyLastVariableUsages)
						handleBlockStates.add(tracker.currentState.copy())
					}
				}
				// Analyse always block (if it exists)
				tracker.setVariableStates(mainBlockState)
				if(unit.alwaysBlock != null) {
					// First analyse for complete execution
					analyseDataFlow(unit.alwaysBlock, tracker)
					val completeExecutionState = tracker.currentState.copy()
					// Then analyse for failure case
					tracker.setVariableStates(initialState)
					tracker.currentState.firstVariableUsages.clear()
					analyseDataFlow(unit.alwaysBlock, tracker)
					tracker.markAllUsagesAsExiting()
					tracker.linkToFirstUsages(potentiallyLastVariableUsages)
					tracker.registerExecutionEnd()
					tracker.setVariableStates(completeExecutionState)
				}
			}
			is StatementBlock -> {
				for(statement in unit.statements)
					analyseDataFlow(statement, tracker)
			}
			is HandleBlock -> {
				if(unit.eventVariable != null)
					tracker.declare(unit.eventVariable)
				analyseDataFlow(unit.block, tracker)
			}
			is IfStatement -> {
				analyseDataFlow(unit.condition, tracker)
				val conditionState = tracker.currentState.copy()
				analyseDataFlow(unit.positiveBranch, tracker)
				val positiveBranchState = tracker.currentState.copy()
				if(unit.negativeBranch == null) {
					tracker.addVariableStates(conditionState)
				} else {
					tracker.setVariableStates(conditionState)
					analyseDataFlow(unit.negativeBranch, tracker)
					tracker.addVariableStates(positiveBranchState)
				}
			}
			is LoopStatement -> {
				if(unit.generator != null)
					analyseDataFlow(unit.generator, tracker)
				val loopStartState = tracker.currentState.copy()
				analyseDataFlow(unit.body, tracker)
				tracker.linkBackTo(loopStartState)
				for(variableState in tracker.nextStatementStates)
					tracker.link(variableState, loopStartState)
				tracker.nextStatementStates.clear()
				if(unit.generator == null)
					tracker.currentState.lastVariableUsages.clear()
				tracker.addVariableStates(*tracker.breakStatementStates.toTypedArray())
				tracker.breakStatementStates.clear()
			}
			is NextStatement -> {
				tracker.registerNextStatement()
			}
			is BreakStatement -> {
				tracker.registerBreakStatement()
			}
			is ReturnStatement -> {
				if(unit.value != null)
					analyseDataFlow(unit.value, tracker)
				tracker.registerExecutionEnd()
			}
			is LocalVariableDeclaration -> {
				tracker.declare(unit)
			}
			is Assignment -> {
				analyseDataFlow(unit.sourceExpression, tracker)
				for(target in unit.targets) {
					if(target is VariableValue) {
						tracker.add(VariableUsage.Type.WRITE, target)
					} else {
						analyseDataFlow(target, tracker)
					}
				}
			}
			is VariableValue -> {
				tracker.add(VariableUsage.Type.READ, unit)
			}
			is BinaryOperator -> {
				analyseDataFlow(unit.left, tracker)
				analyseDataFlow(unit.right, tracker)
			}
			is BinaryModification -> {
				analyseDataFlow(unit.modifier, tracker)
				if(unit.target is VariableValue) {
					tracker.add(listOf(VariableUsage.Type.READ, VariableUsage.Type.MUTATION), unit.target)
				} else {
					analyseDataFlow(unit.target, tracker)
				}
			}
		}
	}

	class VariableTracker {
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

		fun add(type: VariableUsage.Type, variable: VariableValue) = add(listOf(type), variable)

		fun add(types: List<VariableUsage.Type>, variable: VariableValue) {
			val usages = variables.getOrPut(variable.definition ?: return) { LinkedList() }
			val firstUsages = currentState.firstVariableUsages.getOrPut(variable.definition ?: return) { LinkedHashSet() }
			val lastUsages = currentState.lastVariableUsages.getOrPut(variable.definition ?: return) { LinkedHashSet() }
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

		fun linkBackTo(previousState: VariableState) {
			link(currentState.lastVariableUsages, previousState)
		}

		fun link(referenceState: VariableState, previousState: VariableState) {
			link(referenceState.lastVariableUsages, previousState)
		}

		fun link(currentPreviousUsages: HashMap<ValueDeclaration, MutableSet<VariableUsage>>, previousState: VariableState) {
			for((declaration, lastUsages) in currentPreviousUsages) {
				val firstUsages = previousState.lastVariableUsages[declaration]?.firstOrNull()?.nextUsages ?: continue
				for(firstUsage in firstUsages) {
					for(lastUsage in lastUsages) {
						firstUsage.previousUsages.addFirst(lastUsage)
						lastUsage.nextUsages.addFirst(firstUsage)
					}
				}
			}
		}

		fun linkToFirstUsages(usages: HashMap<ValueDeclaration, MutableSet<VariableUsage>>) {
			for((declaration, potentiallyLastUsages) in usages) {
				for(firstUsageInHandleBlock in currentState.firstVariableUsages[declaration] ?: continue) {
					for(potentiallyLastUsage in potentiallyLastUsages) {
						potentiallyLastUsage.nextUsages.add(firstUsageInHandleBlock)
						firstUsageInHandleBlock.previousUsages.add(potentiallyLastUsage)
					}
				}
			}
		}

		fun calculateEndState() {
			for((declaration, usages) in variables) {
				usages.firstOrNull()?.isFirstUsage = true
				for(usage in currentState.lastVariableUsages[declaration] ?: continue)
					usage.isLastUsage = true
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

		fun collectAllUsagesInto(
			potentiallyLastVariableUsages: HashMap<ValueDeclaration, MutableSet<VariableUsage>>
		) {
			var currentUsages = currentState.firstVariableUsages
			while(true) {
				var hasNextUsages = false
				val nextVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
				usagesToBeLinked@ for((declaration, usages) in currentUsages) {
					val potentiallyLastUsages = potentiallyLastVariableUsages.getOrPut(declaration) { HashSet() }
					potentiallyLastUsages.addAll(usages)
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
				var report = "start -> ${usages.first()}"
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
}
