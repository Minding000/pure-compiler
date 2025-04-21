package components.semantic_model.operations

import components.code_generation.llvm.models.operations.BinaryOperator
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.OptionalType
import components.semantic_model.values.*
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.access.AbstractMonomorphicAccess
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.resolution.NotFound
import util.combineOrUnion
import components.code_generation.llvm.models.values.Value as ValueUnit
import components.syntax_parser.syntax_tree.operations.BinaryOperator as BinaryOperatorSyntaxTree

class BinaryOperator(override val source: BinaryOperatorSyntaxTree, scope: Scope, val left: Value, val right: Value,
					 val kind: Operator.Kind): Value(source, scope) {
	var targetSignature: FunctionSignature? = null
	var conversions: Map<Value, InitializerDefinition>? = null
	override val hasGenericType: Boolean
		get() = targetSignature?.original?.returnType != targetSignature?.returnType

	init {
		addSemanticModels(left, right)
	}

	override fun determineTypes() {
		super.determineTypes()
		var leftType = left.effectiveType ?: return
		val rightType = right.effectiveType ?: return
		if(kind == Operator.Kind.DOUBLE_QUESTION_MARK) {
			if(SpecialType.NULL.matches(leftType)) {
				providedType = rightType
			} else {
				val nonOptionalLeftType = if(leftType is OptionalType) leftType.baseType else leftType
				providedType = listOf(nonOptionalLeftType, rightType).combineOrUnion(this)
				addSemanticModels(providedType)
			}
			return
		}
		if(kind == Operator.Kind.EQUAL_TO || kind == Operator.Kind.NOT_EQUAL_TO || kind == Operator.Kind.IDENTICAL_TO
			|| kind == Operator.Kind.NOT_IDENTICAL_TO) {
			//TODO fix: comparing with null as target leads to crash (write tests!)
			if(leftType is OptionalType)
				leftType = leftType.baseType
		}
		try {
			val match = leftType.interfaceScope.getOperator(kind, right)
			if(match == null) {
				context.addIssue(NotFound(source, "Operator", "$leftType $kind ${right.getDisplayType()}"))
				return
			}
			targetSignature = match.signature
			conversions = match.conversions
			setUnextendedType(match.returnType.getLocalType(this, leftType))
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "operator", "$leftType $kind ${right.getDisplayType()}")
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val isConditional = SpecialType.BOOLEAN.matches(left.providedType) && (kind == Operator.Kind.AND || kind == Operator.Kind.OR)
		val isComparison = kind == Operator.Kind.EQUAL_TO || kind == Operator.Kind.NOT_EQUAL_TO
		left.analyseDataFlow(tracker)
		if(isConditional) {
			val isAnd = kind == Operator.Kind.AND
			tracker.setVariableStates(left.getEndState(isAnd))
			val variableValue = left as? VariableValue
			val declaration = variableValue?.declaration
			if(declaration != null) {
				val booleanLiteral = BooleanLiteral(this, isAnd)
				tracker.add(VariableUsage.Kind.HINT, declaration, this, booleanLiteral.providedType, booleanLiteral)
			}
			right.analyseDataFlow(tracker)
			setEndState(right.getEndState(isAnd), isAnd)
			tracker.setVariableStates(left.getEndState(!isAnd))
			tracker.addVariableStates(right.getEndState(!isAnd))
			setEndState(tracker, !isAnd)
			tracker.addVariableStates(getEndState(isAnd))
		} else if(isComparison) {
			right.analyseDataFlow(tracker)
			val variableValue = left as? VariableValue ?: right as? VariableValue
			val literalValue = left as? LiteralValue ?: right as? LiteralValue
			val declaration = variableValue?.declaration
			if(declaration != null && literalValue != null) {
				val isPositive = kind == Operator.Kind.EQUAL_TO
				setEndState(tracker, !isPositive)
				tracker.add(VariableUsage.Kind.HINT, declaration, this, literalValue.providedType, literalValue)
				setEndState(tracker, isPositive)
				tracker.addVariableStates(getEndState(!isPositive))
			} else {
				setEndStates(tracker)
			}
		} else {
			right.analyseDataFlow(tracker)
			setEndStates(tracker)
		}
		computeStaticValue()
	}

	private fun computeStaticValue() {
		staticValue = when(kind) {
			Operator.Kind.DOUBLE_QUESTION_MARK -> {
				val leftValue = left.getComputedValue() ?: return
				if(leftValue is NullLiteral)
					right.getComputedValue()
				else
					leftValue
			}
			Operator.Kind.AND -> {
				val leftValue = left.getComputedValue() as? BooleanLiteral ?: return
				val rightValue = right.getComputedValue() as? BooleanLiteral ?: return
				BooleanLiteral(this, leftValue.value && rightValue.value)
			}
			Operator.Kind.OR -> {
				val leftValue = left.getComputedValue() as? BooleanLiteral ?: return
				val rightValue = right.getComputedValue() as? BooleanLiteral ?: return
				BooleanLiteral(this, leftValue.value || rightValue.value)
			}
			Operator.Kind.PLUS -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				NumberLiteral(this, leftValue.value + rightValue.value)
			}
			Operator.Kind.MINUS -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				NumberLiteral(this, leftValue.value - rightValue.value)
			}
			Operator.Kind.STAR -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				NumberLiteral(this, leftValue.value * rightValue.value)
			}
			Operator.Kind.SLASH -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				//TODO report exceptions on division by zero and overflow
				if(rightValue.value.toDouble() == 0.0)
					return
				NumberLiteral(this, leftValue.value / rightValue.value)
			}
			Operator.Kind.SMALLER_THAN -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				BooleanLiteral(this, leftValue.value < rightValue.value)
			}
			Operator.Kind.GREATER_THAN -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				BooleanLiteral(this, leftValue.value > rightValue.value)
			}
			Operator.Kind.SMALLER_THAN_OR_EQUAL_TO -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				BooleanLiteral(this, leftValue.value <= rightValue.value)
			}
			Operator.Kind.GREATER_THAN_OR_EQUAL_TO -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				BooleanLiteral(this, leftValue.value >= rightValue.value)
			}
			Operator.Kind.IDENTICAL_TO -> {
				val leftValue = left.getComputedValue() ?: return
				val rightValue = right.getComputedValue() ?: return
				BooleanLiteral(this, leftValue == rightValue)
			}
			Operator.Kind.NOT_IDENTICAL_TO -> {
				val leftValue = left.getComputedValue() ?: return
				val rightValue = right.getComputedValue() ?: return
				BooleanLiteral(this, leftValue != rightValue)
			}
			Operator.Kind.EQUAL_TO -> {
				val leftValue = left.getComputedValue() as? LiteralValue ?: return
				val rightValue = right.getComputedValue() as? LiteralValue ?: return
				BooleanLiteral(this, leftValue == rightValue)
			}
			Operator.Kind.NOT_EQUAL_TO -> {
				val leftValue = left.getComputedValue() as? LiteralValue ?: return
				val rightValue = right.getComputedValue() as? LiteralValue ?: return
				BooleanLiteral(this, leftValue != rightValue)
			}
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
	}

	override fun validate() {
		super.validate()
		validateWhereClauseConditions()
		validateMonomorphicAccess()
	}

	private fun validateWhereClauseConditions() {
		val signature = targetSignature ?: return
		val leftType = left.providedType ?: return
		val typeParameters = (leftType as? ObjectType)?.typeParameters ?: emptyList()
		for(condition in signature.whereClauseConditions) {
			if(!condition.isMet(typeParameters))
				context.addIssue(WhereClauseUnfulfilled(source, "Operator",
					signature.original.toString(false, kind), leftType, condition))
		}
	}

	private fun validateMonomorphicAccess() {
		val signature = targetSignature ?: return
		val leftType = left.providedType ?: return
		if(signature.associatedImplementation?.isAbstract == true && signature.associatedImplementation.isMonomorphic
			&& !leftType.isMemberAccessible(signature, true))
			context.addIssue(AbstractMonomorphicAccess(source, "operator",
				signature.toString(false, kind), leftType))
	}

	override fun toUnit(): ValueUnit {
		return BinaryOperator(this, left.toUnit(), right.toUnit())
	}
}
