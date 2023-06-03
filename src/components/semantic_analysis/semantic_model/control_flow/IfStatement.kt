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

	override fun compile(llvmConstructor: LlvmConstructor) {
		val conditionBlock = llvmConstructor.getCurrentBlock()
		val function = llvmConstructor.getParentFunction(conditionBlock)
		val condition = condition.getLlvmReference(llvmConstructor)
		val trueBlock = llvmConstructor.createBlock(function, "if_true")
		val falseBlock = llvmConstructor.createBlock(function, "if_false")
		val exitBlock = llvmConstructor.createBlock("exit")
		llvmConstructor.buildJump(condition, trueBlock, falseBlock)
		llvmConstructor.select(trueBlock)
		positiveBranch.compile(llvmConstructor)
		if(!positiveBranch.isInterruptingExecution)
			llvmConstructor.buildJump(exitBlock)
		llvmConstructor.select(falseBlock)
		negativeBranch?.compile(llvmConstructor)
		if(negativeBranch?.isInterruptingExecution != true)
			llvmConstructor.buildJump(exitBlock)
		if(!(positiveBranch.isInterruptingExecution && negativeBranch?.isInterruptingExecution == true)) {
			llvmConstructor.addBlockToFunction(function, exitBlock)
			llvmConstructor.select(exitBlock)
		}
	}
}
