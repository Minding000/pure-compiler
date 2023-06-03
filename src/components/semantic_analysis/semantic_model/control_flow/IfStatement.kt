package components.semantic_analysis.semantic_model.control_flow

import components.compiler.targets.llvm.LlvmCompilerContext
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.Value
import org.bytedeco.llvm.global.LLVM
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
		val conditionBlock = LLVM.LLVMGetInsertBlock(llvmCompilerContext.builder)
		val functionReference = LLVM.LLVMGetBasicBlockParent(conditionBlock)
		val condition = condition.getLlvmReference(llvmCompilerContext)
		val trueBlock = LLVM.LLVMAppendBasicBlockInContext(llvmCompilerContext.context, functionReference, "if_true")
		val falseBlock = LLVM.LLVMAppendBasicBlockInContext(llvmCompilerContext.context, functionReference, "if_false")
		val exitBlock = LLVM.LLVMCreateBasicBlockInContext(llvmCompilerContext.context, "exit")
		LLVM.LLVMBuildCondBr(llvmCompilerContext.builder, condition, trueBlock, falseBlock)
		LLVM.LLVMPositionBuilderAtEnd(llvmCompilerContext.builder, trueBlock)
		positiveBranch.compile(llvmCompilerContext)
		if(!positiveBranch.isInterruptingExecution)
			LLVM.LLVMBuildBr(llvmCompilerContext.builder, exitBlock)
		LLVM.LLVMPositionBuilderAtEnd(llvmCompilerContext.builder, falseBlock)
		negativeBranch?.compile(llvmCompilerContext)
		if(negativeBranch?.isInterruptingExecution != true)
			LLVM.LLVMBuildBr(llvmCompilerContext.builder, exitBlock)
		if(!(positiveBranch.isInterruptingExecution && negativeBranch?.isInterruptingExecution == true)) {
			LLVM.LLVMAppendExistingBasicBlock(functionReference, exitBlock)
			LLVM.LLVMPositionBuilderAtEnd(llvmCompilerContext.builder, exitBlock)
		}
	}
}
