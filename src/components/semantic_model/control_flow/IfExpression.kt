package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmBlock
import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.OrUnionType
import components.semantic_model.types.Type
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.Value
import errors.internal.CompilerError
import logger.issues.if_expressions.ExpressionNeverReturns
import logger.issues.if_expressions.MissingElse
import logger.issues.if_expressions.MissingValue
import java.util.*
import components.syntax_parser.syntax_tree.control_flow.IfExpression as IfStatementSyntaxTree

class IfExpression(override val source: IfStatementSyntaxTree, scope: Scope, val condition: Value, val positiveBranch: SemanticModel,
				   val negativeBranch: SemanticModel?, val isPartOfExpression: Boolean): Value(source, scope) {
	override var isInterruptingExecution = false
	private var isConditionAlwaysTrue = false
	private var isConditionAlwaysFalse = false

	init {
		addSemanticModels(condition, positiveBranch, negativeBranch)
	}

	override fun determineTypes() {
		super.determineTypes()
		if(!isPartOfExpression)
			return
		val types = LinkedList<Type>()
		val lastStatementInPositiveBranch = getLastStatement(positiveBranch)
		val positiveBranchType = (lastStatementInPositiveBranch as? Value)?.type
		if(positiveBranchType != null)
			types.add(positiveBranchType)
		if(negativeBranch != null) {
			val lastStatementInNegativeBranch = getLastStatement(negativeBranch)
			val negativeBranchType = (lastStatementInNegativeBranch as? Value)?.type
			if(negativeBranchType != null)
				types.add(negativeBranchType)
		}
		if(types.isNotEmpty())
			type = OrUnionType(source, scope, types).simplified()
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		condition.analyseDataFlow(tracker)
		tracker.setVariableStates(condition.getPositiveEndState())
		positiveBranch.analyseDataFlow(tracker)
		if(negativeBranch == null) {
			tracker.addVariableStates(condition.getNegativeEndState())
		} else {
			val positiveBranchState = tracker.currentState.copy()
			tracker.setVariableStates(condition.getNegativeEndState())
			negativeBranch.analyseDataFlow(tracker)
			tracker.addVariableStates(positiveBranchState)
		}
		(condition.getComputedValue() as? BooleanLiteral)?.value.let { staticResult ->
			isConditionAlwaysTrue = staticResult == true
			isConditionAlwaysFalse = staticResult == false
		}
		if(isPartOfExpression) {
			if(isConditionAlwaysTrue)
				staticValue = (positiveBranch as? Value)?.getComputedValue()
			if(isConditionAlwaysFalse)
				staticValue = (negativeBranch as? Value)?.getComputedValue()
		}
	}

	override fun validate() {
		if(isPartOfExpression) {
			super.validate()
		} else {
			for(semanticModel in semanticModels)
				semanticModel.validate()
		}
		isInterruptingExecution = (isConditionAlwaysTrue && positiveBranch.isInterruptingExecution) ||
			(isConditionAlwaysFalse && negativeBranch?.isInterruptingExecution == true) ||
			(positiveBranch.isInterruptingExecution && negativeBranch?.isInterruptingExecution == true)
		validateElseBranchExistence()
		validateValueExistence()
	}

	private fun validateElseBranchExistence() {
		if(!isPartOfExpression)
			return
		if(negativeBranch == null)
			context.addIssue(MissingElse(source))
	}

	private fun validateValueExistence() {
		if(!isPartOfExpression)
			return
		if(positiveBranch.isInterruptingExecution && negativeBranch?.isInterruptingExecution == true) {
			context.addIssue(ExpressionNeverReturns(source))
			return
		}
		val lastStatementInPositiveBranch = getLastStatement(positiveBranch)
		if(!(lastStatementInPositiveBranch is Value || lastStatementInPositiveBranch?.isInterruptingExecution == true))
			context.addIssue(MissingValue(positiveBranch.source))
		if(negativeBranch != null) {
			val lastStatementInNegativeBranch = getLastStatement(negativeBranch)
			if(!(lastStatementInNegativeBranch is Value || lastStatementInNegativeBranch?.isInterruptingExecution == true))
				context.addIssue(MissingValue(negativeBranch.source))
		}
	}

	private fun getLastStatement(branch: SemanticModel): SemanticModel? {
		if(branch is ErrorHandlingContext)
			return branch.mainBlock.statements.lastOrNull()
		return branch
	}

	override fun compile(constructor: LlvmConstructor) {
		val function = constructor.getParentFunction()
		val condition = condition.getLlvmValue(constructor)
		val trueBlock = constructor.createBlock(function, "if_trueBlock")
		val falseBlock = constructor.createBlock(function, "if_falseBlock")
		val exitBlock = constructor.createBlock("if_exitBlock")
		constructor.buildJump(condition, trueBlock, falseBlock)
		constructor.select(trueBlock)
		positiveBranch.compile(constructor)
		if(!positiveBranch.isInterruptingExecution)
			constructor.buildJump(exitBlock)
		constructor.select(falseBlock)
		negativeBranch?.compile(constructor)
		if(negativeBranch?.isInterruptingExecution != true)
			constructor.buildJump(exitBlock)
		if(!(positiveBranch.isInterruptingExecution && negativeBranch?.isInterruptingExecution == true)) {
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val resultLlvmType = type?.getLlvmType(constructor)
		val result = constructor.buildStackAllocation(resultLlvmType, "if_resultVariable")
		val function = constructor.getParentFunction()
		val condition = condition.getLlvmValue(constructor)
		val trueBlock = constructor.createBlock(function, "if_trueBlock")
		val falseBlock = constructor.createBlock(function, "if_falseBlock")
		val exitBlock = constructor.createBlock("if_exitBlock")
		constructor.buildJump(condition, trueBlock, falseBlock)
		constructor.select(trueBlock)
		compileBranch(constructor, positiveBranch, result, exitBlock)
		constructor.select(falseBlock)
		val negativeBranch = negativeBranch ?: throw CompilerError(source, "If expression is missing a negative branch.")
		compileBranch(constructor, negativeBranch, result, exitBlock)
		if(!(positiveBranch.isInterruptingExecution && negativeBranch.isInterruptingExecution)) {
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
		return constructor.buildLoad(resultLlvmType, result, "if_result")
	}

	private fun compileBranch(constructor: LlvmConstructor, branch: SemanticModel, result: LlvmValue, exitBlock: LlvmBlock) {
		if(branch.isInterruptingExecution) {
			branch.compile(constructor)
			return
		}
		when(branch) {
			is ErrorHandlingContext -> {
				val statements = branch.mainBlock.statements
				val lastStatementIndex = statements.size - 1
				for((statementIndex, statement) in statements.withIndex()) {
					if(statementIndex == lastStatementIndex) {
						val value = statement as? Value ?: throw CompilerError(statement.source,
							"Last statement in if expression branch block doesn't provide a value.")
						constructor.buildStore(value.getLlvmValue(constructor), result)
					} else {
						statement.compile(constructor)
					}
				}
			}
			is Value -> {
				constructor.buildStore(branch.getLlvmValue(constructor), result)
			}
			else -> {
				throw CompilerError(branch.source,
					"Branch of if expression doesn't return a value and doesn't interrupt execution.")
			}
		}
		constructor.buildJump(exitBlock)
	}
}
