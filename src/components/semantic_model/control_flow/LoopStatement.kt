package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmBlock
import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.types.FunctionType
import components.semantic_model.types.PluralType
import components.semantic_model.types.Type
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.control_flow.LoopStatement as LoopStatementSyntaxTree

class LoopStatement(override val source: LoopStatementSyntaxTree, override val scope: BlockScope, val generator: SemanticModel?,
					val body: ErrorHandlingContext): SemanticModel(source, scope) {
	override var isInterruptingExecution = false
	var mightGetBrokenOutOf = false
	private val hasFiniteGenerator: Boolean
		get() {
			if(generator == null) {
				return false
			} else if(generator is WhileGenerator) {
				val condition = generator.condition.getComputedValue()
				if(condition is BooleanLiteral && condition.value)
					return false
			}
			return true
		}
	val mutatedVariables = HashSet<ValueDeclaration>()
	private lateinit var entryBlock: LlvmBlock
	private lateinit var exitBlock: LlvmBlock

	init {
		scope.semanticModel = this
		addSemanticModels(generator, body)
	}

	override fun determineTypes() {
		context.surroundingLoops.add(this)
		super.determineTypes()
		context.surroundingLoops.remove(this)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		for(declaration in mutatedVariables)
			tracker.add(VariableUsage.Kind.HINT, declaration, this, declaration.type, null)
		val (loopReferencePoint, loopEndState) = if(generator is WhileGenerator) {
			//TODO fix: does not work if WhileGenerator.isPostCondition
			val referencePoint = tracker.currentState.createReferencePoint()
			generator.analyseDataFlow(tracker)
			tracker.setVariableStates(generator.condition.getPositiveEndState())
			Pair(referencePoint, generator.condition.getNegativeEndState())
		} else {
			generator?.analyseDataFlow(tracker)
			Pair(tracker.currentState.createReferencePoint(), tracker.currentState.copy())
		}
		body.analyseDataFlow(tracker)
		tracker.linkBackTo(loopReferencePoint)
		for(variableState in tracker.nextStatementStates)
			tracker.link(variableState, loopReferencePoint)
		tracker.nextStatementStates.clear()
		if(hasFiniteGenerator) {
			if(generator is WhileGenerator)
				tracker.setVariableStates(loopEndState)
			else
				tracker.addVariableStates(loopEndState)
		} else {
			tracker.currentState.lastVariableUsages.clear()
		}
		tracker.addVariableStates(tracker.breakStatementStates)
		tracker.breakStatementStates.clear()
		tracker.currentState.removeReferencePoint(loopReferencePoint)
	}

	override fun validate() {
		super.validate()
		scope.validate()
		if(!(hasFiniteGenerator || mightGetBrokenOutOf))
			isInterruptingExecution = true
	}

	override fun compile(constructor: LlvmConstructor) {
		if(generator is OverGenerator) {
			val iterableType = generator.iterable.type
			if(iterableType is PluralType)
				compileVariadicOverLoop(constructor, generator, iterableType)
			else
				compileIteratorOverLoop(constructor, generator, iterableType)
			return
		}
		val function = constructor.getParentFunction()
		entryBlock = constructor.createBlock(function, "loop_entry")
		exitBlock = constructor.createBlock("loop_exit")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		if(generator is WhileGenerator) {
			val condition = generator.condition.getLlvmValue(constructor)
			val bodyBlock = constructor.createBlock(function, "loop_body")
			constructor.buildJump(condition, bodyBlock, exitBlock)
			constructor.select(bodyBlock)
		}
		body.compile(constructor)
		if(!body.isInterruptingExecution)
			constructor.buildJump(entryBlock)
		if(generator is WhileGenerator || mightGetBrokenOutOf) {
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
	}

	private fun compileVariadicOverLoop(constructor: LlvmConstructor, generator: OverGenerator, pluralType: PluralType) {
		for(variableDeclaration in generator.variableDeclarations)
			variableDeclaration.compile(constructor)
		val function = constructor.getParentFunction()
		val elementCount = constructor.getLastParameter(function)
		val elementList = constructor.buildStackAllocation(context.variadicParameterListStruct, "_overGeneratorElementList")
		constructor.buildFunctionCall(context.llvmVariableParameterIterationStartFunctionType,
			context.llvmVariableParameterIterationStartFunction, listOf(elementList))
		val indexType = constructor.i32Type
		val indexLocation = generator.currentIndexVariable?.llvmLocation
			?: constructor.buildStackAllocation(indexType, "_overGeneratorIndexLocation")
		constructor.buildStore(constructor.buildInt32(0), indexLocation)
		entryBlock = constructor.createBlock(function, "loop_entry")
		exitBlock = constructor.createBlock("loop_exit")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		val index = constructor.buildLoad(indexType, indexLocation, "_overGeneratorIndex")
		val condition = constructor.buildSignedIntegerLessThan(index, elementCount, "_overGeneratorCondition")
		val bodyBlock = constructor.createBlock(function, "loop_body")
		constructor.buildJump(condition, bodyBlock, exitBlock)
		constructor.select(bodyBlock)
		val valueVariable = generator.currentValueVariable
		if(valueVariable != null) {
			// Variadic parameters are always 64bits in size (at least on Windows).
			// See: https://discourse.llvm.org/t/va-arg-on-windows-64/40780
			val elementMemory = constructor.getCurrentVariadicElement(elementList, constructor.i64Type, "_overGeneratorElementMemory")
			val elementType = pluralType.baseType.getLlvmType(constructor)
			val element = constructor.changeTypeAllowingDataLoss(elementMemory, elementType, "_overGeneratorElement")
			constructor.buildStore(element, valueVariable.llvmLocation)
		}
		body.compile(constructor)
		val newIndex = constructor.buildIntegerAddition(index, constructor.buildInt32(1), "_newOverGeneratorIndex")
		constructor.buildStore(newIndex, indexLocation)
		if(!body.isInterruptingExecution)
			constructor.buildJump(entryBlock)
		constructor.addBlockToFunction(function, exitBlock)
		constructor.select(exitBlock)
		constructor.buildFunctionCall(context.llvmVariableParameterIterationEndFunctionType,
			context.llvmVariableParameterIterationEndFunction, listOf(elementList))
	}

	private fun compileIteratorOverLoop(constructor: LlvmConstructor, generator: OverGenerator, iterableType: Type?) {
		for(variableDeclaration in generator.variableDeclarations)
			variableDeclaration.compile(constructor)

		val iteratorCreationPropertyType = iterableType?.getValueDeclaration("createIterator")?.second
		val iteratorCreationMatch = (iteratorCreationPropertyType as? FunctionType)?.getSignature()
		val iteratorCreationSignature = iteratorCreationMatch?.signature
		val iteratorType = iteratorCreationMatch?.returnType
		val iteratorLlvmType = iteratorType?.getLlvmType(constructor)
		val iteratorAdvancePropertyType = iteratorType?.getValueDeclaration("advance")?.second
		val iteratorAdvanceSignature = (iteratorAdvancePropertyType as? FunctionType)?.getSignature()?.signature

		val function = constructor.getParentFunction()
		val createIteratorAddress = context.resolveFunction(constructor, iterableType?.getLlvmType(constructor),
			generator.iterable.getLlvmValue(constructor), "createIterator(): <Element>List.Iterator") //TODO change function IDs: exclude return type
		val iterator = constructor.buildFunctionCall(iteratorCreationSignature?.getLlvmType(constructor), createIteratorAddress,
			emptyList(), "iterator")
		entryBlock = constructor.createBlock(function, "loop_entry")
		exitBlock = constructor.createBlock("loop_exit")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		val condition = context.resolveMember(constructor, iteratorLlvmType, iterator, "isDone")
		val bodyBlock = constructor.createBlock(function, "loop_body")
		constructor.buildJump(condition, bodyBlock, exitBlock)
		constructor.select(bodyBlock)
		val indexVariable = generator.currentIndexVariable
		if(indexVariable != null) {
			val indexProperty = context.resolveMember(constructor, iteratorLlvmType, iterator, "currentIndex")
			constructor.buildStore(indexProperty, indexVariable.llvmLocation)
		}
		val keyVariable = generator.currentKeyVariable
		if(keyVariable != null) {
			val indexProperty = context.resolveMember(constructor, iteratorLlvmType, iterator, "currentKey")
			constructor.buildStore(indexProperty, keyVariable.llvmLocation)
		}
		val valueVariable = generator.currentValueVariable
		if(valueVariable != null) {
			val indexProperty = context.resolveMember(constructor, iteratorLlvmType, iterator, "currentValue")
			constructor.buildStore(indexProperty, valueVariable.llvmLocation)
		}
		body.compile(constructor)
		val advanceFunctionAddress = context.resolveFunction(constructor, iterableType?.getLlvmType(constructor),
			generator.iterable.getLlvmValue(constructor), "advance()")
		constructor.buildFunctionCall(iteratorAdvanceSignature?.getLlvmType(constructor), advanceFunctionAddress)
		if(!body.isInterruptingExecution)
			constructor.buildJump(entryBlock)
		constructor.addBlockToFunction(function, exitBlock)
		constructor.select(exitBlock)
	}

	fun jumpToNextIteration(constructor: LlvmConstructor) {
		constructor.buildJump(entryBlock)
	}

	fun jumpOut(constructor: LlvmConstructor) {
		constructor.buildJump(exitBlock)
	}
}
