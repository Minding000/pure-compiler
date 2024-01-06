package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
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
		val valueType = target.type ?: return
		try {
			val match = valueType.interfaceScope.getOperator(kind, listOf(modifier))
			if(match == null) {
				context.addIssue(NotFound(source, "Operator", "$valueType $kind ${modifier.type}"))
				return
			}
			targetSignature = match.signature
			if(match.signature.associatedImplementation?.isAbstract == true && match.signature.associatedImplementation.isMonomorphic
				&& !valueType.isMemberAccessible(match.signature, true))
				context.addIssue(AbstractMonomorphicAccess(source, "operator",
					match.signature.toString(false, kind), valueType))
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "operator", "$valueType $kind ${modifier.type}")
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
	}

	private fun validateWhereClauseConditions() {
		val signature = targetSignature ?: return
		val targetType = target.type ?: return
		val typeParameters = (targetType as? ObjectType)?.typeParameters ?: emptyList()
		for(condition in signature.whereClauseConditions) {
			if(!condition.isMet(typeParameters))
				context.addIssue(WhereClauseUnfulfilled(source, "Operator",
					signature.original.toString(false, kind), targetType, condition))
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		super.compile(constructor)
		val isTargetInteger = SpecialType.INTEGER.matches(target.type)
		val isTargetPrimitiveNumber = isTargetInteger || SpecialType.FLOAT.matches(target.type)
		val isModifierInteger = SpecialType.INTEGER.matches(modifier.type)
		val isModifierPrimitiveNumber = isModifierInteger || SpecialType.FLOAT.matches(modifier.type)
		if(isTargetPrimitiveNumber && isModifierPrimitiveNumber) {
			val targetValue = target.getLlvmValue(constructor)
			var modifierValue = modifier.getLlvmValue(constructor)
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
			constructor.buildStore(operation, target.getLlvmLocation(constructor))
		} else {
			val signature = targetSignature?.original ?: throw CompilerError(source, "Binary modification is missing a target.")
			createLlvmFunctionCall(constructor, signature)
		}
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature) {
		val targetValue = target.getLlvmValue(constructor)
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(targetValue)
		parameters.add(modifier.getLlvmValue(constructor))
		val functionAddress = context.resolveFunction(constructor, targetValue,
			signature.original.toString(false, kind))
		constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters)
		context.continueRaise()
	}
}
