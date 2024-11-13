package components.semantic_model.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.operations.UnaryOperator
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.Type
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.NumberLiteral
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.resolution.NotFound
import java.util.*
import components.code_generation.llvm.models.values.Value as ValueUnit
import components.syntax_parser.syntax_tree.operations.UnaryOperator as UnaryOperatorSyntaxTree

//TODO disallow spread operation (triple dot) outside of parameter list-
class UnaryOperator(override val source: UnaryOperatorSyntaxTree, scope: Scope, val subject: Value, val kind: Operator.Kind):
	Value(source, scope) {
	var targetSignature: FunctionSignature? = null
	override val hasGenericType: Boolean
		get() = targetSignature?.original?.returnType != targetSignature?.returnType

	init {
		addSemanticModels(subject)
	}

	override fun determineTypes() {
		super.determineTypes()
		val subjectType = subject.effectiveType ?: return
		try {
			val match = subjectType.interfaceScope.getOperator(kind)
			if(match == null) {
				context.addIssue(NotFound(source, "Operator", "$kind$subjectType"))
				return
			}
			targetSignature = match.signature
			setUnextendedType(match.returnType.getLocalType(this, subjectType))
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "operator", "$kind$subjectType")
		}
	}

	override fun isAssignableTo(targetType: Type?): Boolean {
		//TODO same for boolean literal? e.g. val infer = !true
		if(kind == Operator.Kind.MINUS && subject is NumberLiteral)
			return subject.isAssignableTo(targetType)
		return super.isAssignableTo(targetType)
	}

	override fun setInferredType(inferredType: Type?) {
		if(kind == Operator.Kind.MINUS && subject is NumberLiteral) {
			subject.setInferredType(inferredType)
			providedType = subject.providedType
		} else {
			super.setInferredType(inferredType)
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		if(SpecialType.BOOLEAN.matches(subject.providedType) && kind == Operator.Kind.EXCLAMATION_MARK) {
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

	override fun validate() {
		super.validate()
		validateWhereClauseConditions()
	}

	private fun validateWhereClauseConditions() {
		val signature = targetSignature ?: return
		val subjectType = subject.providedType ?: return
		val typeParameters = (subjectType as? ObjectType)?.typeParameters ?: emptyList()
		for(condition in signature.whereClauseConditions) {
			if(!condition.isMet(typeParameters))
				context.addIssue(WhereClauseUnfulfilled(source, "Operator",
					signature.original.toString(false, kind), subjectType, condition))
		}
	}

	override fun toUnit(): ValueUnit {
		return UnaryOperator(this, subject.toUnit())
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		if(kind == Operator.Kind.MINUS && subject is NumberLiteral)
			return subject.createLlvmValue(constructor, subject.value.negate())
		val resultName = "_unaryOperatorResult"
		val llvmValue = ValueConverter.convertIfRequired(this, constructor, subject.getLlvmValue(constructor), subject.effectiveType,
			subject.hasGenericType, subject.effectiveType, false)
		if(SpecialType.BOOLEAN.matches(subject.providedType)) {
			if(kind == Operator.Kind.EXCLAMATION_MARK)
				return constructor.buildBooleanNegation(llvmValue, resultName)
		} else if(SpecialType.BYTE.matches(subject.providedType) || SpecialType.INTEGER.matches(subject.providedType)) {
			if(kind == Operator.Kind.MINUS)
				return constructor.buildIntegerNegation(llvmValue, resultName)
		} else if(SpecialType.FLOAT.matches(subject.providedType)) {
			if(kind == Operator.Kind.MINUS)
				return constructor.buildFloatNegation(llvmValue, resultName)
		}
		val signature = targetSignature?.original ?: throw CompilerError(source, "Unary operator is missing a target.")
		return createLlvmFunctionCall(constructor, signature, llvmValue)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature, targetValue: LlvmValue): LlvmValue {
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(targetValue)
		val functionAddress = context.resolveFunction(constructor, targetValue, signature.getIdentifier(kind))
		val returnValue = constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters,
			"_unaryOperatorResult")
		context.continueRaise(constructor, this)
		return returnValue
	}
}
