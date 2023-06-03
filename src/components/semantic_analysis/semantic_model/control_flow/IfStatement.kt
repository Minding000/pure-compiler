package components.semantic_analysis.semantic_model.control_flow

import components.compiler.targets.llvm.Llvm
import components.compiler.targets.llvm.LlvmCompilerContext
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

	override fun compile(llvmCompilerContext: LlvmCompilerContext) {
		val conditionBlock = Llvm.getCurrentBlock(llvmCompilerContext)
		val function = Llvm.getParentFunction(conditionBlock)
		val condition = condition.getLlvmReference(llvmCompilerContext)
		val trueBlock = Llvm.createBlock(llvmCompilerContext, function, "if_true")
		val falseBlock = Llvm.createBlock(llvmCompilerContext, function, "if_false")
		val exitBlock = Llvm.createBlock(llvmCompilerContext, "exit")
		Llvm.buildJump(llvmCompilerContext, condition, trueBlock, falseBlock)
		Llvm.appendTo(llvmCompilerContext, trueBlock)
		positiveBranch.compile(llvmCompilerContext)
		if(!positiveBranch.isInterruptingExecution)
			Llvm.buildJump(llvmCompilerContext, exitBlock)
		Llvm.appendTo(llvmCompilerContext, falseBlock)
		negativeBranch?.compile(llvmCompilerContext)
		if(negativeBranch?.isInterruptingExecution != true)
			Llvm.buildJump(llvmCompilerContext, exitBlock)
		if(!(positiveBranch.isInterruptingExecution && negativeBranch?.isInterruptingExecution == true)) {
			Llvm.addBlockToFunction(function, exitBlock)
			Llvm.appendTo(llvmCompilerContext, exitBlock)
		}
	}
}
