package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.general.ErrorHandlingContext
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.wrapper.LlvmBlock
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.control_flow.LoopStatement
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.types.FunctionType
import components.semantic_model.types.PluralType
import components.semantic_model.types.Type
import errors.internal.CompilerError

class LoopStatement(override val model: LoopStatement, val generator: Unit?, val body: ErrorHandlingContext):
	Unit(model, listOfNotNull(generator, body)) {
	private lateinit var entryBlock: LlvmBlock
	private lateinit var exitBlock: LlvmBlock
	private var advance: (() -> kotlin.Unit)? = null

	override fun compile(constructor: LlvmConstructor) {
		if(generator is OverGenerator) {
			val iterableType = generator.iterable.model.effectiveType
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
		if(!body.model.isInterruptingExecutionBasedOnStructure)
			constructor.buildJump(entryBlock)
		if(generator is WhileGenerator || model.mightGetBrokenOutOf) {
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
	}

	private fun compileVariadicOverLoop(constructor: LlvmConstructor, generator: OverGenerator, pluralType: PluralType) {
		for(variableDeclaration in generator.variableDeclarations)
			variableDeclaration.compile(constructor)
		val function = constructor.getParentFunction()
		val elementCount = constructor.getLastParameter(function)
		val elementList = constructor.buildStackAllocation(context.runtimeStructs.variadicParameterList, "_overGenerator_elementList")
		constructor.buildFunctionCall(context.externalFunctions.variableParameterIterationStart, listOf(elementList))
		val indexType = constructor.i32Type
		val indexVariable = generator.model.currentIndexVariable?.unit?.llvmLocation
			?: constructor.buildStackAllocation(indexType, "_overGenerator_indexVariable")
		constructor.buildStore(constructor.buildInt32(0), indexVariable)
		entryBlock = constructor.createBlock(function, "loop_entry")
		exitBlock = constructor.createBlock(function, "loop_exit")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		val index = constructor.buildLoad(indexType, indexVariable, "_overGenerator_index")

		advance = {
			val newIndex = constructor.buildIntegerAddition(index, constructor.buildInt32(1), "_overGenerator_newIndex")
			constructor.buildStore(newIndex, indexVariable)
		}

		val condition = constructor.buildSignedIntegerLessThan(index, elementCount, "_overGenerator_condition")
		val bodyBlock = constructor.createBlock(function, "loop_body")
		constructor.buildJump(condition, bodyBlock, exitBlock)
		constructor.select(bodyBlock)
		val valueVariable = generator.model.currentValueVariable
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
			constructor.buildStore(element, valueVariable.unit.llvmLocation)
		}
		body.compile(constructor)
		if(!body.model.isInterruptingExecutionBasedOnStructure)
			jumpToNextIteration(constructor)
		constructor.select(exitBlock)
		//TODO also call this if the loop body returns / raises? (like always block)
		constructor.buildFunctionCall(context.externalFunctions.variableParameterIterationEnd, listOf(elementList))
	}

	private fun compileIteratorOverLoop(constructor: LlvmConstructor, generator: OverGenerator, iterableType: Type?) {
		for(variableDeclaration in generator.variableDeclarations)
			variableDeclaration.compile(constructor)

		val iteratorCreationPropertyName = "createIterator"
		val iteratorCreationPropertyType = iterableType?.getValueDeclaration(iteratorCreationPropertyName)?.type
		val iteratorCreationMatch = (iteratorCreationPropertyType as? FunctionType)?.getSignature()
		val iteratorCreationSignature = iteratorCreationMatch?.signature
			?: throw CompilerError(model, "'Iterator.createIterator' signature not found.")
		val iteratorType = iteratorCreationMatch.returnType
		val iteratorAdvancePropertyType = iteratorType.getValueDeclaration("advance")?.type
		val iteratorAdvanceSignature = (iteratorAdvancePropertyType as? FunctionType)?.getSignature()?.signature
		val iteratorIsDoneComputedProperty = iteratorType.getValueDeclaration("isDone")?.declaration as? ComputedPropertyDeclaration
			?: throw CompilerError(model, "'Iterator.isDone' computed property not found.")

		val function = constructor.getParentFunction()
		//TODO support looping over union types
		val iterableLlvmValue = generator.iterable.getLlvmValue(constructor)
		val createIteratorAddress =
			context.resolveFunction(constructor, iterableLlvmValue, iteratorCreationSignature.getIdentifier(iteratorCreationPropertyName))
		val exceptionAddress = context.getExceptionParameter(constructor)
		val iteratorLlvmValue = constructor.buildFunctionCall(iteratorCreationSignature.getLlvmType(constructor), createIteratorAddress,
			listOf(exceptionAddress, iterableLlvmValue), "iterator")
		val advanceFunctionAddress = context.resolveFunction(constructor, iteratorLlvmValue, "advance()")

		advance = {
			constructor.buildFunctionCall(iteratorAdvanceSignature?.getLlvmType(constructor), advanceFunctionAddress,
				listOf(exceptionAddress, iteratorLlvmValue))
			context.continueRaise(constructor, model)
		}

		context.continueRaise(constructor, model)
		entryBlock = constructor.createBlock(function, "loop_entry")
		exitBlock = constructor.createBlock(function, "loop_exit")
		context.printDebugLine(constructor, "Iterator created (${generator.model.source.getStartString()})")
		constructor.buildJump(entryBlock)
		constructor.select(entryBlock)
		context.printDebugLine(constructor, "Loop entry block")
		val condition = buildGetterCall(constructor, exceptionAddress, iteratorLlvmValue, iteratorIsDoneComputedProperty)
		val bodyBlock = constructor.createBlock(function, "loop_body")
		constructor.buildJump(condition, exitBlock, bodyBlock)
		constructor.select(bodyBlock)
		context.printDebugLine(constructor, "Loop body block")
		val indexVariable = generator.model.currentIndexVariable
		if(indexVariable != null) {
			val iteratorCurrentIndexComputedProperty = iteratorType.getValueDeclaration("currentIndex")
				?.declaration as? ComputedPropertyDeclaration
				?: throw CompilerError(model, "'Iterator.currentIndex' computed property not found.")
			val currentIndexValue = buildGetterCall(constructor, exceptionAddress, iteratorLlvmValue, iteratorCurrentIndexComputedProperty)
			val convertedValue = ValueConverter.convertIfRequired(model, constructor, currentIndexValue,
				iteratorCurrentIndexComputedProperty.root.getterReturnType, false, generator.model.currentIndexVariable?.effectiveType,
				false)
			constructor.buildStore(convertedValue, indexVariable.unit.llvmLocation)
		}
		val keyVariable = generator.model.currentKeyVariable
		if(keyVariable != null) {
			val iteratorCurrentKeyComputedProperty = iteratorType.getValueDeclaration("currentKey")
				?.declaration as? ComputedPropertyDeclaration
				?: throw CompilerError(model, "'Iterator.currentKey' computed property not found.")
			val currentKeyValue = buildGetterCall(constructor, exceptionAddress, iteratorLlvmValue, iteratorCurrentKeyComputedProperty)
			val convertedValue = ValueConverter.convertIfRequired(model, constructor, currentKeyValue,
				iteratorCurrentKeyComputedProperty.root.getterReturnType, false, generator.model.currentKeyVariable?.effectiveType, false)
			constructor.buildStore(convertedValue, keyVariable.unit.llvmLocation)
		}
		//TODO consider that computed property type behaves like a function return type (can be generic)
		val valueVariable = generator.model.currentValueVariable
		if(valueVariable != null) {
			val iteratorCurrentValueComputedProperty = iteratorType.getValueDeclaration("currentValue")
				?.declaration as? ComputedPropertyDeclaration
				?: throw CompilerError(model, "'Iterator.currentValue' computed property not found.")
			val currentValueValue = buildGetterCall(constructor, exceptionAddress, iteratorLlvmValue, iteratorCurrentValueComputedProperty)
			val convertedValue = ValueConverter.convertIfRequired(model, constructor, currentValueValue,
				iteratorCurrentValueComputedProperty.root.getterReturnType, false, generator.model.currentValueVariable?.effectiveType,
				false)
			constructor.buildStore(convertedValue, valueVariable.unit.llvmLocation)
		}
		body.compile(constructor)
		if(!body.model.isInterruptingExecutionBasedOnStructure)
			jumpToNextIteration(constructor)
		constructor.select(exitBlock)
		context.printDebugLine(constructor, "Loop exit block")
	}

	private fun buildGetterCall(constructor: LlvmConstructor, exceptionAddress: LlvmValue, targetValue: LlvmValue,
								computedPropertyDeclaration: ComputedPropertyDeclaration): LlvmValue {
		val functionAddress = context.resolveFunction(constructor, targetValue, computedPropertyDeclaration.getterIdentifier)
		val returnValue = constructor.buildFunctionCall(computedPropertyDeclaration.computedPropertyUnit.llvmGetterType, functionAddress,
			listOf(exceptionAddress, targetValue), "_computedPropertyGetterResult")
		context.continueRaise(constructor, model)
		return returnValue
	}

	fun jumpToNextIteration(constructor: LlvmConstructor) {
		advance?.invoke()
		constructor.buildJump(entryBlock)
	}

	fun jumpOut(constructor: LlvmConstructor) {
		constructor.buildJump(exitBlock)
	}
}
