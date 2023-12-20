package components.semantic_model.control_flow

import components.code_generation.llvm.*
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.types.*
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.ValueDeclaration
import errors.internal.CompilerError
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
		val elementList = constructor.buildStackAllocation(context.variadicParameterListStruct, "_overGenerator_elementList")
		constructor.buildFunctionCall(context.llvmVariableParameterIterationStartFunctionType,
			context.llvmVariableParameterIterationStartFunction, listOf(elementList))
		val indexType = constructor.i32Type
		val indexVariable = generator.currentIndexVariable?.llvmLocation
			?: constructor.buildStackAllocation(indexType, "_overGenerator_indexVariable")
		constructor.buildStore(constructor.buildInt32(0), indexVariable)
		entryBlock = constructor.createBlock(function, "loop_entry")
		exitBlock = constructor.createBlock(function, "loop_exit")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		val index = constructor.buildLoad(indexType, indexVariable, "_overGenerator_index")
		val condition = constructor.buildSignedIntegerLessThan(index, elementCount, "_overGenerator_condition")
		val bodyBlock = constructor.createBlock(function, "loop_body")
		constructor.buildJump(condition, bodyBlock, exitBlock)
		constructor.select(bodyBlock)
		val valueVariable = generator.currentValueVariable
		if(valueVariable != null) {
			// Variadic parameters are always 64bits in size (at least on Windows).
			// See: https://discourse.llvm.org/t/va-arg-on-windows-64/40780
			val elementMemory = constructor.getCurrentVariadicElement(elementList, constructor.i64Type,
				"_overGenerator_elementMemory")
			val elementType = pluralType.baseType.getLlvmType(constructor)
			val element = if(elementType == constructor.pointerType)
				constructor.buildCastFromIntegerToPointer(elementMemory, "_overGenerator_element")
			else
				constructor.changeTypeAllowingDataLoss(elementMemory, elementType, "_overGenerator_element")
			constructor.buildStore(element, valueVariable.llvmLocation)
		}
		body.compile(constructor)
		if(!body.isInterruptingExecution) {
			//TODO fix: should be called on 'next' statement
			val newIndex = constructor.buildIntegerAddition(index, constructor.buildInt32(1), "_overGenerator_newIndex")
			constructor.buildStore(newIndex, indexVariable)
			constructor.buildJump(entryBlock)
		}
		constructor.select(exitBlock)
		//TODO also call this if the loop body returns / raises? (like always block)
		constructor.buildFunctionCall(context.llvmVariableParameterIterationEndFunctionType,
			context.llvmVariableParameterIterationEndFunction, listOf(elementList))
	}

	private fun compileIteratorOverLoop(constructor: LlvmConstructor, generator: OverGenerator, iterableType: Type?) {
		for(variableDeclaration in generator.variableDeclarations)
			variableDeclaration.compile(constructor)

		val iteratorCreationPropertyType = iterableType?.getValueDeclaration("createIterator")?.type
		val iteratorCreationMatch = (iteratorCreationPropertyType as? FunctionType)?.getSignature()
		val iteratorCreationSignature = iteratorCreationMatch?.signature
		val iteratorType = iteratorCreationMatch?.returnType
		val iteratorLlvmType = (iteratorType as? ObjectType)?.getTypeDeclaration()?.llvmType
		val iteratorAdvancePropertyType = iteratorType?.getValueDeclaration("advance")?.type
		val iteratorAdvanceSignature = (iteratorAdvancePropertyType as? FunctionType)?.getSignature()?.signature
		val iteratorIsDoneComputedProperty = iteratorType?.getValueDeclaration("isDone")?.declaration as? ComputedPropertyDeclaration
			?: throw CompilerError(source, "'Iterator.isDone' computed property not found.")

		val function = constructor.getParentFunction()
		//TODO support looping over union types
		val iterableLlvmValue = generator.iterable.getLlvmValue(constructor)
		val iterableLlvmType = when(iterableType) {
			is ObjectType -> iterableType.getTypeDeclaration()?.llvmType
			is SelfType -> iterableType.typeDeclaration?.llvmType
			else -> null
		}
		val createIteratorAddress = context.resolveFunction(constructor, iterableLlvmType, iterableLlvmValue,
			"createIterator(): <Element>List.Iterator") //TODO change function IDs: exclude return type
		val exceptionAddress = constructor.buildStackAllocation(constructor.pointerType, "__exceptionAddress")
		val iteratorLlvmValue = constructor.buildFunctionCall(iteratorCreationSignature?.getLlvmType(constructor), createIteratorAddress,
			listOf(exceptionAddress, iterableLlvmValue), "iterator")
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
		entryBlock = constructor.createBlock(function, "loop_entry")
		exitBlock = constructor.createBlock(function, "loop_exit")
		context.printDebugMessage(constructor, "Iterator created (${generator.source.getStartString()})")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		context.printDebugMessage(constructor, "Loop entry block")
		val condition = buildGetterCall(constructor, iteratorLlvmType, iteratorLlvmValue, iteratorIsDoneComputedProperty)
		val bodyBlock = constructor.createBlock(function, "loop_body")
		constructor.buildJump(condition, exitBlock, bodyBlock)
		constructor.select(bodyBlock)
		context.printDebugMessage(constructor, "Loop body block")
		val indexVariable = generator.currentIndexVariable
		if(indexVariable != null) {
			val iteratorCurrentIndexComputedProperty = iteratorType.getValueDeclaration("currentIndex")
				?.declaration as? ComputedPropertyDeclaration
				?: throw CompilerError(source, "'Iterator.currentIndex' computed property not found.")
			val currentIndexValue = buildGetterCall(constructor, iteratorLlvmType, iteratorLlvmValue, iteratorCurrentIndexComputedProperty)
			val convertedValue = ValueConverter.convertIfRequired(this, constructor, currentIndexValue,
				iteratorCurrentIndexComputedProperty.type, generator.currentIndexVariable?.type)
			constructor.buildStore(convertedValue, indexVariable.llvmLocation)
		}
		val keyVariable = generator.currentKeyVariable
		if(keyVariable != null) {
			val iteratorCurrentKeyComputedProperty = iteratorType.getValueDeclaration("currentKey")
				?.declaration as? ComputedPropertyDeclaration
				?: throw CompilerError(source, "'Iterator.currentKey' computed property not found.")
			val currentKeyValue = buildGetterCall(constructor, iteratorLlvmType, iteratorLlvmValue, iteratorCurrentKeyComputedProperty)
			val convertedValue = ValueConverter.convertIfRequired(this, constructor, currentKeyValue,
				iteratorCurrentKeyComputedProperty.type, generator.currentKeyVariable?.type)
			constructor.buildStore(convertedValue, keyVariable.llvmLocation)
		}
		val valueVariable = generator.currentValueVariable
		if(valueVariable != null) {
			val iteratorCurrentValueComputedProperty = iteratorType.getValueDeclaration("currentValue")
				?.declaration as? ComputedPropertyDeclaration
				?: throw CompilerError(source, "'Iterator.currentValue' computed property not found.")
			val currentValueValue = buildGetterCall(constructor, iteratorLlvmType, iteratorLlvmValue, iteratorCurrentValueComputedProperty)
			val convertedValue = ValueConverter.convertIfRequired(this, constructor, currentValueValue,
				iteratorCurrentValueComputedProperty.type, generator.currentValueVariable?.type)
			constructor.buildStore(convertedValue, valueVariable.llvmLocation)
		}
		body.compile(constructor)
		if(!body.isInterruptingExecution) {
			//TODO fix: should be called on 'next' statement
			val advanceFunctionAddress = context.resolveFunction(constructor, iteratorLlvmType, iteratorLlvmValue, "advance()")
			constructor.buildFunctionCall(iteratorAdvanceSignature?.getLlvmType(constructor), advanceFunctionAddress,
				listOf(exceptionAddress, iteratorLlvmValue))
			//TODO if exception exists
			// check for optional try (normal and force try have no effect)
			// check for catch
			// resume raise
			constructor.buildJump(entryBlock)
		}
		constructor.select(exitBlock)
		context.printDebugMessage(constructor, "Loop exit block")
	}

	private fun buildGetterCall(constructor: LlvmConstructor, targetType: LlvmType?, targetValue: LlvmValue,
								computedPropertyDeclaration: ComputedPropertyDeclaration): LlvmValue {
		val functionAddress = context.resolveFunction(constructor, targetType, targetValue, computedPropertyDeclaration.getterIdentifier)
		val exceptionAddress = constructor.buildStackAllocation(constructor.pointerType, "__exceptionAddress")
		return constructor.buildFunctionCall(computedPropertyDeclaration.llvmGetterType, functionAddress,
			listOf(exceptionAddress, targetValue), "_computedPropertyGetterResult")
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
	}

	fun jumpToNextIteration(constructor: LlvmConstructor) {
		constructor.buildJump(entryBlock)
	}

	fun jumpOut(constructor: LlvmConstructor) {
		constructor.buildJump(exitBlock)
	}
}
