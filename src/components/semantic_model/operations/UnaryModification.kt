package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.values.NumberLiteral
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import java.math.BigDecimal
import java.util.*
import components.syntax_parser.syntax_tree.operations.UnaryModification as UnaryModificationSyntaxTree

class UnaryModification(override val source: UnaryModificationSyntaxTree, scope: Scope, val target: Value, val kind: Operator.Kind):
	SemanticModel(source, scope) {
	var targetSignature: FunctionSignature? = null

	companion object {
		private val STEP_SIZE = BigDecimal(1)
	}

	init {
		addSemanticModels(target)
	}

	override fun determineTypes() {
		super.determineTypes()
		context.registerWrite(target)
		val targetType = target.type ?: return
		try {
			val match = targetType.interfaceScope.getOperator(kind)
			if(match == null) {
				context.addIssue(NotFound(source, "Operator", "$targetType$kind"))
				return
			}
			targetSignature = match.signature
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "operator", "$targetType$kind")
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
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
		val resultingValue = when(kind) {
			Operator.Kind.DOUBLE_PLUS -> targetValue + STEP_SIZE
			Operator.Kind.DOUBLE_MINUS -> targetValue - STEP_SIZE
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
		return NumberLiteral(this, resultingValue)
	}

	override fun compile(constructor: LlvmConstructor) {
		super.compile(constructor)
		if(SpecialType.INTEGER.matches(target.type)) {
			val targetValue = target.getLlvmValue(constructor)
			val modifierValue = constructor.buildInt32(STEP_SIZE.longValueExact())
			val intermediateResultName = "_modifiedValue"
			val operation = when(kind) {
				Operator.Kind.DOUBLE_PLUS -> constructor.buildIntegerAddition(targetValue, modifierValue, intermediateResultName)
				Operator.Kind.DOUBLE_MINUS -> constructor.buildIntegerSubtraction(targetValue, modifierValue, intermediateResultName)
				else -> throw CompilerError(source, "Unknown native unary integer modification of kind '$kind'.")
			}
			constructor.buildStore(operation, target.getLlvmLocation(constructor))
			return
		}
		val signature = targetSignature?.original ?: throw CompilerError(source, "Unary modification is missing a target.")
		createLlvmFunctionCall(constructor, signature)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature) {
		val typeDefinition = signature.parentDefinition
		val targetValue = target.getLlvmValue(constructor)
		val exceptionAddressLocation = constructor.buildStackAllocation(constructor.pointerType, "exceptionAddress")
		val parameters = LinkedList<LlvmValue>()
		parameters.add(exceptionAddressLocation)
		parameters.add(targetValue)
		val functionAddress = context.resolveFunction(constructor, typeDefinition?.llvmType, targetValue,
			signature.toString(false, kind))
		constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters)
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
	}
}
