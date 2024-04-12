package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.ValueConverter
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.FunctionSignature
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

	init {
		addSemanticModels(target, modifier)
	}

	override fun determineTypes() {
		super.determineTypes()
		context.registerWrite(target)
		val targetType = target.effectiveType ?: return
		val modifierType = target.effectiveType ?: return
		try {
			val match = targetType.interfaceScope.getOperator(kind, modifier)
			if(match == null) {
				context.addIssue(NotFound(source, "Operator", "$targetType $kind $modifierType"))
				return
			}
			targetSignature = match.signature
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
			Operator.Kind.SLASH_EQUALS -> targetValue / modifierValue
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

	override fun compile(constructor: LlvmConstructor) {
		super.compile(constructor)
		val targetValue = ValueConverter.convertIfRequired(this, constructor, target.getLlvmValue(constructor),
			target.effectiveType, target.hasGenericType, target.effectiveType, false)
		val modifierType = targetSignature?.parameterTypes?.firstOrNull() ?: modifier.effectiveType
		val originalModifierType = targetSignature?.original?.parameterTypes?.firstOrNull() ?: modifier.effectiveType
		var modifierValue = ValueConverter.convertIfRequired(this, constructor, modifier.getLlvmValue(constructor),
			modifier.effectiveType, modifier.hasGenericType, modifierType, modifierType != originalModifierType)
		val isTargetInteger = SpecialType.INTEGER.matches(target.providedType)
		val isTargetPrimitiveNumber = isTargetInteger || SpecialType.FLOAT.matches(target.providedType)
		val isModifierInteger = SpecialType.INTEGER.matches(modifier.providedType)
		val isModifierPrimitiveNumber = isModifierInteger || SpecialType.FLOAT.matches(modifier.providedType)
		if(isTargetPrimitiveNumber && isModifierPrimitiveNumber) {
			val isIntegerOperation = isTargetInteger && isModifierInteger
			if(!isIntegerOperation) {
				if(isTargetInteger)
					throw CompilerError(source, "Integer target with float modifier in binary modification.")
				else if(isModifierInteger)
					modifierValue = constructor.buildCastFromSignedIntegerToFloat(modifierValue, "_implicitlyCastBinaryModifier")
			}
			val intermediateResultName = "_modifiedValue"
			val operation = when(kind) {
				Operator.Kind.PLUS_EQUALS -> {
					if(isIntegerOperation)
						constructor.buildIntegerAddition(targetValue, modifierValue, intermediateResultName)
					else
						constructor.buildFloatAddition(targetValue, modifierValue, intermediateResultName)
				}
				Operator.Kind.MINUS_EQUALS -> {
					if(isIntegerOperation)
						constructor.buildIntegerSubtraction(targetValue, modifierValue, intermediateResultName)
					else
						constructor.buildFloatSubtraction(targetValue, modifierValue, intermediateResultName)
				}
				Operator.Kind.STAR_EQUALS -> {
					if(isIntegerOperation)
						constructor.buildIntegerMultiplication(targetValue, modifierValue, intermediateResultName)
					else
						constructor.buildFloatMultiplication(targetValue, modifierValue, intermediateResultName)
				}
				Operator.Kind.SLASH_EQUALS -> {
					if(isIntegerOperation)
						constructor.buildSignedIntegerDivision(targetValue, modifierValue, intermediateResultName)
					else
						constructor.buildFloatDivision(targetValue, modifierValue, intermediateResultName)
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
		val functionAddress = context.resolveFunction(constructor, targetValue,
			signature.original.toString(false, kind))
		constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters)
		context.continueRaise(constructor)
	}
}
