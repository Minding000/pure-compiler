package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.OrUnionType
import components.semantic_model.types.Type
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import logger.issues.expressions.BranchMissesValue
import logger.issues.expressions.ExpressionMissesElse
import logger.issues.expressions.ExpressionNeverReturns
import logger.issues.switches.CaseTypeMismatch
import logger.issues.switches.DuplicateCase
import logger.issues.switches.NoCases
import logger.issues.switches.RedundantElse
import java.util.*
import components.syntax_parser.syntax_tree.control_flow.SwitchExpression as SwitchStatementSyntaxTree

class SwitchExpression(override val source: SwitchStatementSyntaxTree, scope: Scope, val subject: Value, val cases: List<Case>,
					   val elseBranch: SemanticModel?, val isPartOfExpression: Boolean): Value(source, scope) {
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
			val lastStatementInCaseBranch = getLastStatement(case.result)
			val caseBranchType = (lastStatementInCaseBranch as? Value)?.type
			if(caseBranchType != null)
				types.add(caseBranchType)
		}
		if(elseBranch != null) {
			val lastStatementInElseBranch = getLastStatement(elseBranch)
			val elseBranchType = (lastStatementInElseBranch as? Value)?.type
			if(elseBranchType != null)
				types.add(elseBranchType)
		}
		if(types.isNotEmpty())
			type = OrUnionType(source, scope, types).simplified()
	}

	private fun inferCaseConditionTypes() {
		val subjectType = subject.type
		for(case in cases) {
			if(!case.condition.isAssignableTo(subjectType)) {
				val conditionType = case.condition.type
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
		evaluateExecutionFlow()
	}

	private fun evaluateExecutionFlow() {
		val areAllBranchesInterruptingExecutionBasedOnStructure = cases.all { case -> case.result.isInterruptingExecutionBasedOnStructure }
			&& elseBranch?.isInterruptingExecutionBasedOnStructure ?: true
		isInterruptingExecutionBasedOnStructure = areAllBranchesInterruptingExecutionBasedOnStructure && isExhaustive()
		val areAllBranchesInterruptingExecutionBasedOnStaticEvaluation = cases.all { case -> case.result.isInterruptingExecutionBasedOnStaticEvaluation }
			&& elseBranch?.isInterruptingExecutionBasedOnStaticEvaluation ?: true
		isInterruptingExecutionBasedOnStaticEvaluation = (getBranchForValue(subject.getComputedValue())?.isInterruptingExecutionBasedOnStaticEvaluation ?: false)
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
			val caseWithUniqueCondition = casesWithUniqueConditions.find {
					caseWithUniqueCondition -> caseWithUniqueCondition.condition == case.condition }
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
			val lastStatementInCaseBranch = getLastStatement(case.result)
			if(!(lastStatementInCaseBranch is Value || lastStatementInCaseBranch?.isInterruptingExecutionBasedOnStructure == true))
				context.addIssue(BranchMissesValue(case.result.source, EXPRESSION_TYPE))
		}
		if(elseBranch != null) {
			val lastStatementInElseBranch = getLastStatement(elseBranch)
			if(!(lastStatementInElseBranch is Value || lastStatementInElseBranch?.isInterruptingExecutionBasedOnStructure == true))
				context.addIssue(BranchMissesValue(elseBranch.source, EXPRESSION_TYPE))
		}
	}

	private fun getLastStatement(branch: SemanticModel): SemanticModel? {
		if(branch is ErrorHandlingContext)
			return branch.mainBlock.statements.lastOrNull()
		return branch
	}

	private fun getBranchForValue(value: Value?): SemanticModel? {
		for(case in cases) {
			if(case.condition.getComputedValue() == value)
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
//		if(subject.type.definition is Enum) {
//			for(instance in enum.instances) {
//				if(!casesContainValue(instance))
//					return false
//			}
//			return true
//		}
		return false
	}

	//TODO compile switch statements
	override fun compile(constructor: LlvmConstructor) {
		super.compile(constructor)
	}

	//TODO compile switch expressions
	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return super.buildLlvmValue(constructor)
	}
}
