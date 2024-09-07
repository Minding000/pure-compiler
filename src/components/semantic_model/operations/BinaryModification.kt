package components.semantic_model.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.values.NumberLiteral
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.access.AbstractMonomorphicAccess
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.resolution.NotFound
import java.util.*
import components.syntax_parser.syntax_tree.operations.BinaryModification as BinaryModificationSyntaxTree

class BinaryModification(override val source: BinaryModificationSyntaxTree, scope: Scope, val target: Value, val modifier: Value,
						 val kind: Operator.Kind): SemanticModel(source, scope) {
	var targetSignature: FunctionSignature? = null
	var conversions: Map<Value, InitializerDefinition>? = null

	init {
		addSemanticModels(target, modifier)
	}

	override fun determineTypes() {
		super.determineTypes()
		context.registerWrite(target)
		val targetType = target.effectiveType ?: return
		val modifierType = modifier.effectiveType ?: return
		try {
			val match = targetType.interfaceScope.getOperator(kind, modifier)
			if(match == null) {
				context.addIssue(NotFound(source, "Operator", "$targetType $kind $modifierType"))
				return
			}
			targetSignature = match.signature
			conversions = match.conversions
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "operator", "$targetType $kind $modifierType")
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		modifier.analyseDataFlow(tracker)
		if(target is VariableValue) {
			target.computeValue(tracker)
			tracker.add(listOf(VariableUsage.Kind.READ, VariableUsage.Kind.MUTATION), target, tracker.getCurrentTypeOf(target.declaration),
				getComputedTargetValue())
		} else {
			target.analyseDataFlow(tracker)
		}
	}

	private fun getComputedTargetValue(): Value? {
		val targetValue = (target.getComputedValue() as? NumberLiteral ?: return null).value
		val modifierValue = (modifier.getComputedValue() as? NumberLiteral ?: return null).value
		val resultingValue = when(kind) {
			Operator.Kind.PLUS_EQUALS -> targetValue + modifierValue
			Operator.Kind.MINUS_EQUALS -> targetValue - modifierValue
			Operator.Kind.STAR_EQUALS -> targetValue * modifierValue
			Operator.Kind.SLASH_EQUALS -> {
				if(modifierValue.toDouble() == 0.0)
					return null
				targetValue / modifierValue
			}
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
		return NumberLiteral(this, resultingValue)
	}

	override fun validate() {
		super.validate()
		validateWhereClauseConditions()
		validateMonomorphicAccess()
	}

	private fun validateWhereClauseConditions() {
		val signature = targetSignature ?: return
		val targetType = target.providedType ?: return
		val typeParameters = (targetType as? ObjectType)?.typeParameters ?: emptyList()
		for(condition in signature.whereClauseConditions) {
			if(!condition.isMet(typeParameters))
				context.addIssue(WhereClauseUnfulfilled(source, "Operator",
					signature.original.toString(false, kind), targetType, condition))
		}
	}

	private fun validateMonomorphicAccess() {
		val signature = targetSignature ?: return
		val valueType = target.providedType ?: return
		if(signature.associatedImplementation?.isAbstract == true && signature.associatedImplementation.isMonomorphic
			&& !valueType.isMemberAccessible(signature, true))
			context.addIssue(AbstractMonomorphicAccess(source, "operator",
				signature.toString(false, kind), valueType))
	}

	//TODO test optional target (also for other operators)
	override fun compile(constructor: LlvmConstructor) {
		val targetValue = ValueConverter.convertIfRequired(this, constructor, target.getLlvmValue(constructor),
			target.effectiveType, target.hasGenericType, target.effectiveType, false)
		val modifierType = targetSignature?.parameterTypes?.firstOrNull() ?: modifier.effectiveType
		val originalModifierType = targetSignature?.original?.parameterTypes?.firstOrNull() ?: modifier.effectiveType
		var modifierValue = ValueConverter.convertIfRequired(this, constructor, modifier.getLlvmValue(constructor),
			modifier.effectiveType, modifier.hasGenericType, modifierType, modifierType != originalModifierType,
			conversions?.get(modifier))
		val isTargetFloat = SpecialType.FLOAT.matches(target.effectiveType)
		val isTargetInteger = SpecialType.INTEGER.matches(target.effectiveType)
		val isTargetPrimitiveNumber = SpecialType.BYTE.matches(target.effectiveType) || isTargetInteger || isTargetFloat
		val isModifierFloat = SpecialType.FLOAT.matches(modifier.effectiveType)
		val isModifierInteger = SpecialType.INTEGER.matches(modifier.effectiveType)
		val isModifierPrimitiveNumber = SpecialType.BYTE.matches(modifier.effectiveType) || isModifierInteger || isModifierFloat
		if(isTargetPrimitiveNumber && isModifierPrimitiveNumber) {
			val isFloatOperation = isTargetFloat || isModifierFloat
			val isIntegerOperation = isTargetInteger || isModifierInteger
			if(isFloatOperation) {
				if(isTargetInteger)
					throw CompilerError(source, "Integer target with float modifier in binary modification.")
				else if(isModifierInteger)
					modifierValue = constructor.buildCastFromSignedIntegerToFloat(modifierValue, "_implicitlyCastBinaryModifier")
			} else if(isIntegerOperation) {
				if(!isTargetInteger)
					throw CompilerError(source, "Byte target with integer modifier in binary modification.")
				else if(!isModifierInteger)
					modifierValue = constructor.buildCastFromByteToInteger(modifierValue, "_implicitlyCastBinaryModifier")
			}
			val intermediateResultName = "_modifiedValue"
			val operation = when(kind) {
				Operator.Kind.PLUS_EQUALS -> {
					if(isFloatOperation)
						constructor.buildFloatAddition(targetValue, modifierValue, intermediateResultName)
					else
						constructor.buildIntegerAddition(targetValue, modifierValue, intermediateResultName)
				}
				Operator.Kind.MINUS_EQUALS -> {
					if(isFloatOperation)
						constructor.buildFloatSubtraction(targetValue, modifierValue, intermediateResultName)
					else
						constructor.buildIntegerSubtraction(targetValue, modifierValue, intermediateResultName)
				}
				Operator.Kind.STAR_EQUALS -> {
					if(isFloatOperation)
						constructor.buildFloatMultiplication(targetValue, modifierValue, intermediateResultName)
					else
						constructor.buildIntegerMultiplication(targetValue, modifierValue, intermediateResultName)
				}
				Operator.Kind.SLASH_EQUALS -> {
					val noDivisionByZeroBlock = constructor.createBlock("validDivision")
					val divisionByZeroBlock = constructor.createBlock("divisionByZero")
					val previousBlock = constructor.getCurrentBlock()
					constructor.select(divisionByZeroBlock)
					if(context.nativeRegistry.has(SpecialType.EXCEPTION)) {
						context.raiseException(constructor, this, "Division by zero")
					} else {
						context.panic(constructor, "Division by zero")
						constructor.markAsUnreachable()
					}
					constructor.select(previousBlock)
					if(isFloatOperation) {
						val isDivisorZero = constructor.buildFloatEqualTo(modifierValue, constructor.buildFloat(0.0), "isDivisorZero")
						constructor.buildJump(isDivisorZero, divisionByZeroBlock, noDivisionByZeroBlock)
						constructor.select(noDivisionByZeroBlock)
						constructor.buildFloatDivision(targetValue, modifierValue, intermediateResultName)
					} else {
						val zero = if(isIntegerOperation)
							constructor.buildInt32(0)
						else
							constructor.buildByte(0)
						val isDivisorZero = constructor.buildSignedIntegerEqualTo(modifierValue, zero, "isDivisorZero")
						constructor.buildJump(isDivisorZero, divisionByZeroBlock, noDivisionByZeroBlock)
						constructor.select(noDivisionByZeroBlock)
						val noOverflowBlock = constructor.createBlock("noOverflowBlock")
						val overflowBlock = constructor.createBlock("overflowBlock")
						val negativeMin = if(isIntegerOperation)
							constructor.buildInt32(Int.MIN_VALUE)
						else
							constructor.buildByte(Byte.MIN_VALUE)
						val isDividendNegativeMin = constructor.buildSignedIntegerEqualTo(targetValue, negativeMin, "isDividendNegativeMin")
						val negativeOne = if(isIntegerOperation)
							constructor.buildInt32(-1)
						else
							constructor.buildByte(-1)
						val isDivisorNegativeOne = constructor.buildSignedIntegerEqualTo(modifierValue, negativeOne, "isDivisorNegativeOne")
						val doesDivisionOverflow = constructor.buildAnd(isDividendNegativeMin, isDivisorNegativeOne, "doesDivisionOverflow")
						constructor.buildJump(doesDivisionOverflow, overflowBlock, noOverflowBlock)
						constructor.select(overflowBlock)
						if(context.nativeRegistry.has(SpecialType.EXCEPTION)) {
							context.raiseException(constructor, this, "Division overflowed")
						} else {
							context.panic(constructor, "Division overflowed")
							constructor.markAsUnreachable()
						}
						constructor.select(noOverflowBlock)
						constructor.buildSignedIntegerDivision(targetValue, modifierValue, intermediateResultName)
					}
				}
				else -> throw CompilerError(source, "Unknown native unary integer modification of kind '$kind'.")
			}
			constructor.buildStore(ValueConverter.convertIfRequired(this, constructor, operation, target.effectiveType,
				false, target.effectiveType, target.hasGenericType), target.getLlvmLocation(constructor))
			return
		}
		val signature = targetSignature?.original ?: throw CompilerError(source, "Binary modification is missing a target.")
		createLlvmFunctionCall(constructor, signature, targetValue, modifierValue)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature, targetValue: LlvmValue,
									   modifierValue: LlvmValue) {
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(targetValue)
		parameters.add(modifierValue)
		val functionAddress = context.resolveFunction(constructor, targetValue, signature.getIdentifier(kind))
		constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters)
		context.continueRaise(constructor, this)
	}
}
