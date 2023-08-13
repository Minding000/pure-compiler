package components.semantic_analysis.semantic_model.control_flow

import components.compiler.targets.llvm.LlvmConstructor
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.IfStatement as IfStatementSyntaxTree

class IfStatement(override val source: IfStatementSyntaxTree, scope: Scope, val condition: Value, val positiveBranch: SemanticModel,
				  val negativeBranch: SemanticModel?): SemanticModel(source, scope) {
	override var isInterruptingExecution = false
	private var isConditionAlwaysTrue = false
	private var isConditionAlwaysFalse = false

	init {
		addSemanticModels(condition, positiveBranch, negativeBranch)
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
	}

	override fun validate() {
		super.validate()
		isInterruptingExecution = (isConditionAlwaysTrue && positiveBranch.isInterruptingExecution) ||
			(isConditionAlwaysFalse && negativeBranch?.isInterruptingExecution == true) ||
			(positiveBranch.isInterruptingExecution && negativeBranch?.isInterruptingExecution == true)
	}

	override fun compile(constructor: LlvmConstructor) {
		val function = constructor.getParentFunction()
		val condition = condition.getLlvmValue(constructor)
		val trueBlock = constructor.createBlock(function, "if_true")
		val falseBlock = constructor.createBlock(function, "if_false") //TODO write test for nested if statements
		val exitBlock = constructor.createBlock("if_exit")
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
}
