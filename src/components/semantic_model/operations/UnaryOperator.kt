package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.scopes.Scope
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.NumberLiteral
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import java.util.*
import components.syntax_parser.syntax_tree.operations.UnaryOperator as UnaryOperatorSyntaxTree

class UnaryOperator(override val source: UnaryOperatorSyntaxTree, scope: Scope, val subject: Value, val kind: Operator.Kind):
	Value(source, scope) {
	var targetSignature: FunctionSignature? = null

	init {
		addSemanticModels(subject)
	}

	override fun determineTypes() {
		super.determineTypes()
		val subjectType = subject.type ?: return
		try {
			val match = subjectType.interfaceScope.getOperator(kind)
			if(match == null) {
				context.addIssue(NotFound(source, "Operator", "$kind$subjectType"))
				return
			}
			targetSignature = match.signature
			type = match.returnType
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "operator", "$kind$subjectType")
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		if(SpecialType.BOOLEAN.matches(subject.type) && kind == Operator.Kind.EXCLAMATION_MARK) {
			positiveState = subject.getNegativeEndState()
			negativeState = subject.getPositiveEndState()
		}
		computeStaticValue()
	}

	private fun computeStaticValue() {
		staticValue = when(kind) {
			Operator.Kind.BRACKETS_GET -> null
			Operator.Kind.EXCLAMATION_MARK -> {
				val booleanValue = subject.getComputedValue() as? BooleanLiteral ?: return
				BooleanLiteral(this, !booleanValue.value)
			}
			Operator.Kind.TRIPLE_DOT -> null
			Operator.Kind.MINUS -> {
				val numberValue = subject.getComputedValue() as? NumberLiteral ?: return
				NumberLiteral(this, -numberValue.value)
			}
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val resultName = "_unaryOperatorResult"
		val llvmValue = subject.getLlvmValue(constructor)
		if(SpecialType.BOOLEAN.matches(subject.type)) {
			if(kind == Operator.Kind.EXCLAMATION_MARK)
				return constructor.buildBooleanNegation(llvmValue, resultName)
		} else if(SpecialType.INTEGER.matches(subject.type)) {
			if(kind == Operator.Kind.MINUS)
				return constructor.buildIntegerNegation(llvmValue, resultName)
		} else if(SpecialType.FLOAT.matches(subject.type)) {
			if(kind == Operator.Kind.MINUS)
				return constructor.buildFloatNegation(llvmValue, resultName)
		}
		val signature = targetSignature?.original ?: throw CompilerError(source, "Unary operator is missing a target.")
		return createLlvmFunctionCall(constructor, signature)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature): LlvmValue {
		val typeDefinition = signature.parentDefinition
		val targetValue = subject.getLlvmValue(constructor)
		val parameters = LinkedList<LlvmValue>()
		parameters.add(targetValue)
		val functionAddress = context.resolveFunction(constructor, typeDefinition?.llvmType, targetValue,
			signature.toString(false, kind))
		return constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters, "_unaryOperatorResult")
	}
}
