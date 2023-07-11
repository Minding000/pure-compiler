package components.semantic_analysis.semantic_model.operations

import components.compiler.targets.llvm.LlvmConstructor
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.NumberLiteral
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.operations.BinaryModification as BinaryModificationSyntaxTree

class BinaryModification(override val source: BinaryModificationSyntaxTree, scope: Scope, val target: Value, val modifier: Value,
						 val kind: Operator.Kind): SemanticModel(source, scope) {

	init {
		addSemanticModels(target, modifier)
	}

	override fun determineTypes() {
		super.determineTypes()
		context.registerWrite(target)
		target.type?.let { valueType ->
			try {
				val operatorDefinition = valueType.interfaceScope.resolveOperator(kind, listOf(modifier))
				if(operatorDefinition == null)
					context.addIssue(NotFound(source, "Operator", "$valueType $kind ${modifier.type}"))
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(source, "operator", "$valueType $kind ${modifier.type}")
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		modifier.analyseDataFlow(tracker)
		if(target is VariableValue) {
			target.computeValue(tracker)
			tracker.add(listOf(VariableUsage.Kind.READ, VariableUsage.Kind.MUTATION), target, tracker.getCurrentTypeOf(target.definition),
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

	override fun compile(constructor: LlvmConstructor) {
		super.compile(constructor)
		if(target is VariableValue) {
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
						modifierValue = constructor.buildCastFromSignedIntegerToFloat(modifierValue, "cast operand to match operation")
				}
				val operation = when(kind) {
					Operator.Kind.PLUS_EQUALS -> {
						if(isIntegerOperation)
							constructor.buildIntegerAddition(targetValue, modifierValue, "integer addition assignment")
						else
							constructor.buildFloatAddition(targetValue, modifierValue, "float addition assignment")
					}
					Operator.Kind.MINUS_EQUALS -> {
						if(isIntegerOperation)
							constructor.buildIntegerSubtraction(targetValue, modifierValue, "integer subtraction assignment")
						else
							constructor.buildFloatSubtraction(targetValue, modifierValue, "float subtraction assignment")
					}
					Operator.Kind.STAR_EQUALS -> {
						if(isIntegerOperation)
							constructor.buildIntegerMultiplication(targetValue, modifierValue, "integer multiplication assignment")
						else
							constructor.buildFloatMultiplication(targetValue, modifierValue, "float multiplication assignment")
					}
					Operator.Kind.SLASH_EQUALS -> {
						if(isIntegerOperation)
							constructor.buildSignedIntegerDivision(targetValue, modifierValue, "integer division assignment")
						else
							constructor.buildFloatDivision(targetValue, modifierValue, "float division assignment")
					}
					else -> throw CompilerError(source, "Unknown native unary integer modification of kind '$kind'.")
				}
				constructor.buildStore(operation, target.definition?.llvmLocation)
			}
		}
	}
}
