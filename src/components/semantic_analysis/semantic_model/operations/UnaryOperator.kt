package components.semantic_analysis.semantic_model.operations

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.declarations.FunctionSignature
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.NumberLiteral
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.Value
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.operations.UnaryOperator as UnaryOperatorSyntaxTree

class UnaryOperator(override val source: UnaryOperatorSyntaxTree, scope: Scope, val subject: Value, val kind: Operator.Kind):
	Value(source, scope) {
	var targetSignature: FunctionSignature? = null

	init {
		addSemanticModels(subject)
	}

	override fun determineTypes() {
		super.determineTypes()
		subject.type?.let { valueType ->
			try {
				targetSignature = valueType.interfaceScope.getOperator(kind)
				if(targetSignature == null) {
					context.addIssue(NotFound(source, "Operator", "$kind$valueType"))
					return@let
				}
				type = targetSignature?.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(source, "operator", "$kind$valueType")
			}
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
		TODO("Unary '$kind${subject.type}' operator is not implemented yet.")
	}
}
