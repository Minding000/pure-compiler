package components.semantic_model.control_flow

import components.code_generation.llvm.wrapper.LlvmBlock
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.Type
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.LiteralValue
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import errors.internal.CompilerError
import logger.issues.expressions.BranchMissesValue
import logger.issues.expressions.ExpressionMissesElse
import logger.issues.expressions.ExpressionNeverReturns
import logger.issues.switches.CaseTypeMismatch
import logger.issues.switches.DuplicateCase
import logger.issues.switches.NoCases
import logger.issues.switches.RedundantElse
import util.combineOrUnion
import java.util.*
import components.syntax_parser.syntax_tree.control_flow.SwitchExpression as SwitchStatementSyntaxTree

class SwitchExpression(override val source: SwitchStatementSyntaxTree, scope: Scope, val subject: Value, val cases: List<Case>,
					   val elseBranch: ErrorHandlingContext?, val isPartOfExpression: Boolean): Value(source, scope) {
	override var isInterruptingExecutionBasedOnStructure = false
	override var isInterruptingExecutionBasedOnStaticEvaluation = false

	companion object {
		const val EXPRESSION_TYPE = "switch"
	}

	init {
		addSemanticModels(subject, elseBranch)
		addSemanticModels(cases)
	}

	override fun determineTypes() {
		super.determineTypes()
		inferCaseConditionTypes()
		if(!isPartOfExpression)
			return
		val types = LinkedList<Type>()
		for(case in cases) {
			val caseBranchType = case.result.getValue()?.providedType
			if(caseBranchType != null)
				types.add(caseBranchType)
		}
		if(elseBranch != null) {
			val elseBranchType = elseBranch.getValue()?.providedType
			if(elseBranchType != null)
				types.add(elseBranchType)
		}
		if(types.isNotEmpty())
			providedType = types.combineOrUnion(this)
	}

	private fun inferCaseConditionTypes() {
		val subjectType = subject.providedType
		for(case in cases) {
			if(!case.condition.isAssignableTo(subjectType)) {
				val conditionType = case.condition.providedType
				if(conditionType != null && subjectType != null)
					context.addIssue(CaseTypeMismatch(source, conditionType, subjectType))
				continue
			}
			case.condition.setInferredType(subjectType)
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		subject.analyseDataFlow(tracker)
		val caseStates = LinkedList<VariableTracker.VariableState>()
		val variableValue = subject as? VariableValue
		val subjectDeclaration = variableValue?.declaration
		for(case in cases) {
			case.condition.analyseDataFlow(tracker)
			val negativeState = tracker.currentState.copy()
			if(subjectDeclaration != null) {
				tracker.add(VariableUsage.Kind.HINT, subjectDeclaration, case, case.condition.getComputedType(),
					case.condition.getComputedValue())
			}
			case.result.analyseDataFlow(tracker)
			caseStates.add(tracker.currentState.copy())
			tracker.setVariableStates(negativeState)
		}
		elseBranch?.analyseDataFlow(tracker)
		tracker.addVariableStates(caseStates)
		if(isPartOfExpression)
			computeStaticValue()
		evaluateExecutionFlow()
	}

	private fun computeStaticValue() {
		val subjectValue = subject.getComputedValue() as? LiteralValue
		if(subjectValue != null) {
			var areAllCasesAlwaysFalse = true
			for(case in cases) {
				val caseValue = case.condition.getComputedValue() as? LiteralValue
				if(caseValue == null) {
					areAllCasesAlwaysFalse = false
					continue
				}
				val isAlwaysTrue = subjectValue == caseValue
				if(isAlwaysTrue) {
					areAllCasesAlwaysFalse = false
					staticValue = case.result.getValue()?.getComputedValue()
					break
				}
			}
			if(areAllCasesAlwaysFalse)
				staticValue = elseBranch?.getValue()?.getComputedValue()
		}
	}

	private fun evaluateExecutionFlow() {
		val areAllBranchesInterruptingExecutionBasedOnStructure = cases.all { case -> case.result.isInterruptingExecutionBasedOnStructure }
			&& elseBranch?.isInterruptingExecutionBasedOnStructure ?: true
		isInterruptingExecutionBasedOnStructure = areAllBranchesInterruptingExecutionBasedOnStructure && isExhaustive()
		val areAllBranchesInterruptingExecutionBasedOnStaticEvaluation =
			cases.all { case -> case.result.isInterruptingExecutionBasedOnStaticEvaluation }
				&& elseBranch?.isInterruptingExecutionBasedOnStaticEvaluation ?: true
		isInterruptingExecutionBasedOnStaticEvaluation =
			(getBranchForValue(subject.getComputedValue())?.isInterruptingExecutionBasedOnStaticEvaluation ?: false)
				|| (isExhaustive() && areAllBranchesInterruptingExecutionBasedOnStaticEvaluation)
	}

	override fun validate() {
		super.validate()
		validateEmptySwitch()
		validateUniqueCases()
		validateRedundantElseBranch()
		validateElseBranchExistence()
		validateValueExistence()
	}

	private fun validateEmptySwitch() {
		if(cases.isEmpty())
			context.addIssue(NoCases(source))
	}

	private fun validateUniqueCases() {
		val casesWithUniqueConditions = LinkedList<Case>()
		for(case in cases) {
			val caseWithUniqueCondition =
				casesWithUniqueConditions.find { caseWithUniqueCondition -> caseWithUniqueCondition.condition == case.condition }
			if(caseWithUniqueCondition != null) {
				context.addIssue(DuplicateCase(caseWithUniqueCondition, case))
				continue
			}
			casesWithUniqueConditions.add(case)
		}
	}

	private fun validateRedundantElseBranch() {
		if(elseBranch != null && isExhaustiveWithoutElseBranch())
			context.addIssue(RedundantElse(elseBranch.source))
	}

	private fun validateElseBranchExistence() {
		if(!isPartOfExpression)
			return
		if(elseBranch == null && !isExhaustiveWithoutElseBranch())
			context.addIssue(ExpressionMissesElse(source, EXPRESSION_TYPE))
	}

	private fun validateValueExistence() {
		if(!isPartOfExpression)
			return
		if(isInterruptingExecutionBasedOnStructure) {
			context.addIssue(ExpressionNeverReturns(source, EXPRESSION_TYPE))
			return
		}
		for(case in cases) {
			val lastStatementInCaseBranch = case.result.getLastStatement()
			if(!(lastStatementInCaseBranch is Value || lastStatementInCaseBranch?.isInterruptingExecutionBasedOnStructure == true))
				context.addIssue(BranchMissesValue(case.result.source, EXPRESSION_TYPE))
		}
		if(elseBranch != null) {
			val lastStatementInElseBranch = elseBranch.getLastStatement()
			if(!(lastStatementInElseBranch is Value || lastStatementInElseBranch?.isInterruptingExecutionBasedOnStructure == true))
				context.addIssue(BranchMissesValue(elseBranch.source, EXPRESSION_TYPE))
		}
	}

	private fun getBranchForValue(value: Value?): SemanticModel? {
		for(case in cases) {
			if(case.condition.getComputedValue() as? LiteralValue == value)
				return case.result
		}
		return null
	}

	private fun isExhaustive(): Boolean {
		if(elseBranch != null)
			return true
		if(isExhaustiveWithoutElseBranch())
			return true
		return false
	}

	private fun isExhaustiveWithoutElseBranch(): Boolean {
		if(subject.getComputedValue() is BooleanLiteral) {
			var containsYes = false
			var containsNo = false
			for(case in cases) {
				if((case.condition as? BooleanLiteral)?.value == true)
					containsYes = true
				if((case.condition as? BooleanLiteral)?.value == false)
					containsNo = true
			}
			return containsYes && containsNo
		}
		//TODO detect exhausted enum values by implementing the following pseudo code:
		//if(subject.type.definition is Enum) {
		//	for(instance in enum.instances) {
		//		if(!casesContainValue(instance))
		//			return false
		//	}
		//	return true
		//}
		return false
	}

	override fun compile(constructor: LlvmConstructor) {
		val function = constructor.getParentFunction()
		val elseBlock = constructor.createBlock(function, "switch_elseBlock")
		val exitBlock = constructor.createDetachedBlock("switch_exitBlock")
		if(cases.isNotEmpty()) {
			val targetBlocks = LinkedList<LlvmBlock>()
			for(case in cases)
				targetBlocks.add(constructor.createBlock(function, "switch_caseConditionBlock"))
			targetBlocks.add(elseBlock)
			val subjectValue = subject.getLlvmValue(constructor)
			constructor.buildJump(targetBlocks.first())
			for((caseIndex, case) in cases.withIndex()) {
				val condition = buildEquals(constructor, case.condition.getLlvmValue(constructor), subjectValue)
				val currentConditionBlock = targetBlocks[caseIndex]
				val nextTargetBlock = targetBlocks[caseIndex + 1]
				constructor.select(currentConditionBlock)
				val caseBodyBlock = constructor.createBlock(function, "switch_caseBodyBlock")
				constructor.buildJump(condition, caseBodyBlock, nextTargetBlock)
				constructor.select(caseBodyBlock)
				case.result.compile(constructor)
				if(!case.result.isInterruptingExecutionBasedOnStructure)
					constructor.buildJump(exitBlock)
			}
			constructor.select(elseBlock)
		}
		if(elseBranch == null) {
			if(isInterruptingExecutionBasedOnStructure) {
				context.panic(constructor, "Exhaustive switch statement did not match any case!")
				constructor.markAsUnreachable()
			}
		} else {
			elseBranch.compile(constructor)
		}
		if(!isInterruptingExecutionBasedOnStructure) {
			if(elseBranch?.isInterruptingExecutionBasedOnStructure != true)
				constructor.buildJump(exitBlock)
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val resultLlvmType = effectiveType?.getLlvmType(constructor)
		val result = constructor.buildStackAllocation(resultLlvmType, "switch_resultVariable")
		val function = constructor.getParentFunction()
		val elseBlock = constructor.createBlock(function, "switch_elseBlock")
		val exitBlock = constructor.createDetachedBlock("switch_exitBlock")
		if(cases.isNotEmpty()) {
			val targetBlocks = LinkedList<LlvmBlock>()
			for(case in cases)
				targetBlocks.add(constructor.createBlock(function, "switch_caseConditionBlock"))
			targetBlocks.add(elseBlock)
			val subjectValue = subject.getLlvmValue(constructor)
			constructor.buildJump(targetBlocks.first())
			for((caseIndex, case) in cases.withIndex()) {
				val condition = buildEquals(constructor, case.condition.getLlvmValue(constructor), subjectValue)
				val currentConditionBlock = targetBlocks[caseIndex]
				val nextTargetBlock = targetBlocks[caseIndex + 1]
				constructor.select(currentConditionBlock)
				val caseBodyBlock = constructor.createBlock(function, "switch_caseBodyBlock")
				constructor.buildJump(condition, caseBodyBlock, nextTargetBlock)
				constructor.select(caseBodyBlock)
				compileBranch(constructor, case.result, result, exitBlock)
			}
			constructor.select(elseBlock)
		}
		if(elseBranch == null) {
			if(isInterruptingExecutionBasedOnStructure) {
				context.panic(constructor, "Exhaustive switch statement did not match any case!")
				constructor.markAsUnreachable()
			} else {
				constructor.buildJump(exitBlock)
			}
		} else {
			compileBranch(constructor, elseBranch, result, exitBlock)
		}
		if(!isInterruptingExecutionBasedOnStructure) {
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
		return constructor.buildLoad(resultLlvmType, result, "switch_result")
	}

	private fun buildEquals(constructor: LlvmConstructor, leftValue: LlvmValue, rightValue: LlvmValue): LlvmValue {
		//TODO this check needs to work for any type
		return constructor.buildBooleanEqualTo(leftValue, rightValue, "switch_case_condition_result")
	}

	private fun compileBranch(constructor: LlvmConstructor, branch: ErrorHandlingContext, result: LlvmValue, exitBlock: LlvmBlock) {
		if(branch.isInterruptingExecutionBasedOnStructure) {
			branch.compile(constructor)
			return
		}
		val statements = branch.mainBlock.statements
		val lastStatementIndex = statements.size - 1
		for((statementIndex, statement) in statements.withIndex()) {
			if(statementIndex == lastStatementIndex) {
				val value = statement as? Value ?: throw CompilerError(statement.source,
					"Last statement in switch expression branch block doesn't provide a value.")
				constructor.buildStore(value.getLlvmValue(constructor), result)
			} else {
				statement.compile(constructor)
			}
		}
		constructor.buildJump(exitBlock)
	}
}
