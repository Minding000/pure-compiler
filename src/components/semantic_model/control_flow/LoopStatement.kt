package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmBlock
import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.ValueConverter
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.types.FunctionType
import components.semantic_model.types.PluralType
import components.semantic_model.types.Type
import components.semantic_model.values.BooleanLiteral
import errors.internal.CompilerError
import components.syntax_parser.syntax_tree.control_flow.LoopStatement as LoopStatementSyntaxTree

class LoopStatement(override val source: LoopStatementSyntaxTree, override val scope: BlockScope, val generator: SemanticModel?,
					val body: ErrorHandlingContext): SemanticModel(source, scope) {
	override var isInterruptingExecutionBasedOnStructure = false
	override var isInterruptingExecutionBasedOnStaticEvaluation = false
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
			tracker.add(VariableUsage.Kind.HINT, declaration, this, declaration.providedType, null)
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
		if(!(hasFiniteGenerator || mightGetBrokenOutOf)) {
			isInterruptingExecutionBasedOnStructure = true
			isInterruptingExecutionBasedOnStaticEvaluation = true
		}
	}

	override fun validate() {
		super.validate()
		scope.validate()
	}

	override fun compile(constructor: LlvmConstructor) {
		if(generator is OverGenerator) {
			val iterableType = generator.iterable.effectiveType
			if(iterableType is PluralType)
				compileVariadicOverLoop(constructor, generator, iterableType)
			else
				compileIteratorOverLoop(constructor, generator, iterableType)
			return
		}
		val function = constructor.getParentFunction()
		entryBlock = constructor.createBlock(function, "loop_entry")
		exitBlock = constructor.createDetachedBlock("loop_exit")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		if(generator is WhileGenerator) {
			val condition = generator.condition.getLlvmValue(constructor)
			val bodyBlock = constructor.createBlock(function, "loop_body")
			constructor.buildJump(condition, bodyBlock, exitBlock)
			constructor.select(bodyBlock)
		}
		body.compile(constructor)
		if(!body.isInterruptingExecutionBasedOnStructure)
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
		if(!body.isInterruptingExecutionBasedOnStructure) {
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

		val iteratorCreationPropertyName = "createIterator"
		val iteratorCreationPropertyType = iterableType?.getValueDeclaration(iteratorCreationPropertyName)?.type
		val iteratorCreationMatch = (iteratorCreationPropertyType as? FunctionType)?.getSignature()
		val iteratorCreationSignature = iteratorCreationMatch?.signature
			?: throw CompilerError(source, "'Iterator.createIterator' signature not found.")
		val iteratorType = iteratorCreationMatch.returnType
		val iteratorAdvancePropertyType = iteratorType.getValueDeclaration("advance")?.type
		val iteratorAdvanceSignature = (iteratorAdvancePropertyType as? FunctionType)?.getSignature()?.signature
		val iteratorIsDoneComputedProperty = iteratorType.getValueDeclaration("isDone")?.declaration as? ComputedPropertyDeclaration
			?: throw CompilerError(source, "'Iterator.isDone' computed property not found.")

		val function = constructor.getParentFunction()
		//TODO support looping over union types
		val iterableLlvmValue = generator.iterable.getLlvmValue(constructor)
		val createIteratorAddress =
			context.resolveFunction(constructor, iterableLlvmValue, iteratorCreationSignature.getIdentifier(iteratorCreationPropertyName))
		val exceptionAddress = context.getExceptionParameter(constructor)
		val iteratorLlvmValue = constructor.buildFunctionCall(iteratorCreationSignature.getLlvmType(constructor), createIteratorAddress,
			listOf(exceptionAddress, iterableLlvmValue), "iterator")
		context.continueRaise(constructor, this)
		entryBlock = constructor.createBlock(function, "loop_entry")
		exitBlock = constructor.createBlock(function, "loop_exit")
		context.printDebugLine(constructor, "Iterator created (${generator.source.getStartString()})")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		context.printDebugLine(constructor, "Loop entry block")
		val condition = buildGetterCall(constructor, exceptionAddress, iteratorLlvmValue, iteratorIsDoneComputedProperty)
		val bodyBlock = constructor.createBlock(function, "loop_body")
		constructor.buildJump(condition, exitBlock, bodyBlock)
		constructor.select(bodyBlock)
		context.printDebugLine(constructor, "Loop body block")
		val indexVariable = generator.currentIndexVariable
		if(indexVariable != null) {
			val iteratorCurrentIndexComputedProperty = iteratorType.getValueDeclaration("currentIndex")
				?.declaration as? ComputedPropertyDeclaration
				?: throw CompilerError(source, "'Iterator.currentIndex' computed property not found.")
			val currentIndexValue = buildGetterCall(constructor, exceptionAddress, iteratorLlvmValue, iteratorCurrentIndexComputedProperty)
			val convertedValue = ValueConverter.convertIfRequired(this, constructor, currentIndexValue,
				iteratorCurrentIndexComputedProperty.providedType, generator.currentIndexVariable?.providedType)
			constructor.buildStore(convertedValue, indexVariable.llvmLocation)
		}
		val keyVariable = generator.currentKeyVariable
		if(keyVariable != null) {
			val iteratorCurrentKeyComputedProperty = iteratorType.getValueDeclaration("currentKey")
				?.declaration as? ComputedPropertyDeclaration
				?: throw CompilerError(source, "'Iterator.currentKey' computed property not found.")
			val currentKeyValue = buildGetterCall(constructor, exceptionAddress, iteratorLlvmValue, iteratorCurrentKeyComputedProperty)
			val convertedValue = ValueConverter.convertIfRequired(this, constructor, currentKeyValue,
				iteratorCurrentKeyComputedProperty.providedType, generator.currentKeyVariable?.providedType)
			constructor.buildStore(convertedValue, keyVariable.llvmLocation)
		}
		val valueVariable = generator.currentValueVariable
		if(valueVariable != null) {
			val iteratorCurrentValueComputedProperty = iteratorType.getValueDeclaration("currentValue")
				?.declaration as? ComputedPropertyDeclaration
				?: throw CompilerError(source, "'Iterator.currentValue' computed property not found.")
			val currentValueValue = buildGetterCall(constructor, exceptionAddress, iteratorLlvmValue, iteratorCurrentValueComputedProperty)
			val convertedValue = ValueConverter.convertIfRequired(this, constructor, currentValueValue,
				iteratorCurrentValueComputedProperty.providedType, generator.currentValueVariable?.providedType)
			constructor.buildStore(convertedValue, valueVariable.llvmLocation)
		}
		body.compile(constructor)
		if(!body.isInterruptingExecutionBasedOnStructure) {
			//TODO fix: should be called on 'next' statement
			val advanceFunctionAddress = context.resolveFunction(constructor, iteratorLlvmValue, "advance()")
			constructor.buildFunctionCall(iteratorAdvanceSignature?.getLlvmType(constructor), advanceFunctionAddress,
				listOf(exceptionAddress, iteratorLlvmValue))
			context.continueRaise(constructor, this)
			constructor.buildJump(entryBlock)
		}
		constructor.select(exitBlock)
		context.printDebugLine(constructor, "Loop exit block")
	}

	private fun buildGetterCall(constructor: LlvmConstructor, exceptionAddress: LlvmValue, targetValue: LlvmValue,
								computedPropertyDeclaration: ComputedPropertyDeclaration): LlvmValue {
		val functionAddress = context.resolveFunction(constructor, targetValue, computedPropertyDeclaration.getterIdentifier)
		val returnValue = constructor.buildFunctionCall(computedPropertyDeclaration.llvmGetterType, functionAddress,
			listOf(exceptionAddress, targetValue), "_computedPropertyGetterResult")
		context.continueRaise(constructor, this)
		return returnValue
	}

	fun jumpToNextIteration(constructor: LlvmConstructor) {
		constructor.buildJump(entryBlock)
	}

	fun jumpOut(constructor: LlvmConstructor) {
		constructor.buildJump(exitBlock)
	}
}
