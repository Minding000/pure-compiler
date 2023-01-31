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
				val initialState = tracker.createVariableState()
				analyseDataFlow(unit.mainBlock, tracker)
				val mainBlockState = tracker.createVariableState()
				//TODO:
				// - the last usage in the main block is not supposed to be linked to the handle blocks
				//   -> but the logic for linking all other usages in the main block to the handle blocks currently depends on this behaviour
				//     -> include firstUsages in VariableState? How?
				//       -> record first usage since last VariableState creation
				val handleBlockStates = LinkedList<VariableTracker.VariableState>()
				for(handleBlock in unit.handleBlocks) {
					analyseDataFlow(handleBlock, tracker)
					handleBlockStates.add(tracker.createVariableState())
					tracker.setVariableStates(mainBlockState)
				}
				for((declaration, lastUsagesBeforeMainBlock) in initialState.previousVariableUsages) {
					for(lastUsageBeforeHandleBlocks in mainBlockState.previousVariableUsages[declaration] ?: continue) {
						for(firstUsageInHandleBlock in lastUsageBeforeHandleBlocks.nextUsages) {
							for(lastUsageBeforeMainBlock in lastUsagesBeforeMainBlock) {
								linkUsageThread(lastUsageBeforeMainBlock, firstUsageInHandleBlock)
							}
						}
					}
				}
				if(unit.alwaysBlock != null) {
					tracker.addVariableStates(*handleBlockStates.toTypedArray())
					analyseDataFlow(unit.alwaysBlock, tracker)
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
				val conditionState = tracker.createVariableState()
				analyseDataFlow(unit.positiveBranch, tracker)
				val positiveBranchState = tracker.createVariableState()
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
				val previousState = tracker.createVariableState()
				analyseDataFlow(unit.body, tracker)
				tracker.linkBackTo(previousState)
				for(variableState in tracker.nextStatementStates)
					tracker.link(variableState, previousState)
				tracker.nextStatementStates.clear()
				if(unit.generator == null)
					tracker.registerExecutionEnd()
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
				tracker.registerReturnStatement()
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

	private fun linkUsageThread(usage: VariableUsage, target: VariableUsage) {
		if(!usage.nextUsages.contains(target)) {
			for(nextUsage in usage.nextUsages)
				linkUsageThread(nextUsage, target)
			usage.nextUsages.add(target)
			target.previousUsages.add(usage)
		}
	}

	class VariableTracker {
		val childTrackers = HashMap<String, VariableTracker>()
		val variableUsages = HashMap<ValueDeclaration, MutableList<VariableUsage>>()
		val previousVariableUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
		val nextStatementStates = LinkedList<VariableState>()
		val breakStatementStates = LinkedList<VariableState>()
		val returnStatementStates = LinkedList<VariableState>()

		fun createVariableState(): VariableState {
			val previousUsages = HashMap<ValueDeclaration, MutableSet<VariableUsage>>()
			for((declaration, usages) in previousVariableUsages)
				previousUsages[declaration] = HashSet(usages)
			return VariableState(previousUsages)
		}

		fun setVariableStates(vararg variableStates: VariableState) {
			previousVariableUsages.clear()
			addVariableStates(*variableStates)
		}

		fun addVariableStates(vararg variableStates: VariableState) {
			for(variableState in variableStates) {
				for((declaration, previousUsages) in variableState.previousVariableUsages) {
					val usages = previousVariableUsages.getOrPut(declaration) { LinkedHashSet() }
					usages.addAll(previousUsages)
				}
			}
		}

		fun declare(declaration: ValueDeclaration) {
			val usages = variableUsages.getOrPut(declaration) { LinkedList() }
			val previousUsages = previousVariableUsages.getOrPut(declaration) { LinkedHashSet() }
			val types = mutableListOf(VariableUsage.Type.DECLARATION)
			if(declaration.value != null)
				types.add(VariableUsage.Type.WRITE)
			val usage = VariableUsage(types, declaration)
			usages.add(usage)
			previousUsages.clear()
			previousUsages.add(usage)
		}

		fun add(type: VariableUsage.Type, variable: VariableValue) = add(listOf(type), variable)

		fun add(types: List<VariableUsage.Type>, variable: VariableValue) {
			val usages = variableUsages.getOrPut(variable.definition ?: return) { LinkedList() }
			val previousUsages = previousVariableUsages.getOrPut(variable.definition ?: return) { LinkedHashSet() }
			val usage = VariableUsage(types, variable)
			for(previousUsage in previousUsages) {
				usage.previousUsages.add(previousUsage)
				previousUsage.nextUsages.add(usage)
			}
			usages.add(usage)
			previousUsages.clear()
			previousUsages.add(usage)
		}

		fun registerExecutionEnd() {
			previousVariableUsages.clear()
		}

		fun registerNextStatement() {
			nextStatementStates.add(createVariableState())
			registerExecutionEnd()
		}

		fun registerBreakStatement() {
			breakStatementStates.add(createVariableState())
			registerExecutionEnd()
		}

		fun registerReturnStatement() {
			returnStatementStates.add(createVariableState())
			registerExecutionEnd()
		}

		fun linkBackTo(previousState: VariableState) {
			link(previousVariableUsages, previousState)
		}

		fun link(referenceState: VariableState, previousState: VariableState) {
			link(referenceState.previousVariableUsages, previousState)
		}

		fun link(currentPreviousUsages: HashMap<ValueDeclaration, MutableSet<VariableUsage>>, previousState: VariableState) {
			for((declaration, lastUsages) in currentPreviousUsages) {
				val firstUsages = previousState.previousVariableUsages[declaration]?.firstOrNull()?.nextUsages ?: continue
				for(firstUsage in firstUsages) {
					for(lastUsage in lastUsages) {
						firstUsage.previousUsages.addFirst(lastUsage)
						lastUsage.nextUsages.addFirst(firstUsage)
					}
				}
			}
		}

		fun calculateEndState() {
			addVariableStates(*returnStatementStates.toTypedArray())
			for((declaration, usages) in variableUsages) {
				usages.firstOrNull()?.isFirstUsage = true
				for(usage in previousVariableUsages[declaration] ?: continue)
					usage.isLastUsage = true
			}
		}

		fun addChild(name: String, fileTracker: VariableTracker) {
			childTrackers[name] = fileTracker
		}

		fun getReport(variableName: String): String? {
			for((declaration, usages) in variableUsages) {
				if(variableName != declaration.name)
					continue
				var report = "start -> ${usages.first().usage.source.start.line.number}"
				for(usage in usages) {
					val lineNumber = usage.usage.source.start.line.number
					val typeString = usage.types.joinToString(" & ").lowercase()
					var targetString = usage.nextUsages.joinToString { variableUsage ->
						variableUsage.usage.source.start.line.number.toString() }
					if(usage.isLastUsage) {
						if(targetString.isNotEmpty())
							targetString += ", "
						targetString += "end"
					}
					report += "\n$lineNumber: $typeString -> $targetString"
				}
				return report
			}
			return null
		}

		class VariableState(val previousVariableUsages: HashMap<ValueDeclaration, MutableSet<VariableUsage>>)
	}

	class VariableUsage(val types: List<Type>, val usage: Unit) {
		var previousUsages = LinkedList<VariableUsage>()
		var nextUsages = LinkedList<VariableUsage>()
		var isFirstUsage = false
		var isLastUsage = false

		enum class Type {
			DECLARATION,
			READ,
			WRITE,
			MUTATION
		}
	}
}
