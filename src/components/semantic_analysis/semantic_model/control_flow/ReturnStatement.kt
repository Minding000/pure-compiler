package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import logger.issues.returns.RedundantReturnValue
import logger.issues.returns.ReturnStatementMissingValue
import logger.issues.returns.ReturnStatementOutsideOfCallable
import logger.issues.returns.ReturnValueTypeMismatch
import components.syntax_parser.syntax_tree.control_flow.ReturnStatement as ReturnStatementSyntaxTree

class ReturnStatement(override val source: ReturnStatementSyntaxTree, scope: Scope, val value: Value?): Value(source, scope) {
	var targetFunction: FunctionImplementation? = null
	override val isInterruptingExecution = true

	init {
		addUnits(value)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		val surroundingFunction = scope.getSurroundingFunction()
		if(surroundingFunction == null) {
			linter.addIssue(ReturnStatementOutsideOfCallable(source))
			return
		}
		targetFunction = surroundingFunction
		surroundingFunction.mightReturnValue = true
		val returnType = surroundingFunction.signature.returnType
		if(value == null) {
			if(!Linter.SpecialType.NOTHING.matches(returnType))
				linter.addIssue(ReturnStatementMissingValue(source))
		} else {
			if(Linter.SpecialType.NOTHING.matches(returnType)) {
				linter.addIssue(RedundantReturnValue(source))
			} else if(value.isAssignableTo(returnType)) {
				value.setInferredType(returnType)
			} else {
				linter.addIssue(ReturnValueTypeMismatch(source))
			}
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		value?.analyseDataFlow(linter, tracker)
		tracker.registerReturnStatement()
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return if(value == null)
//			LLVMBuildRetVoid(context.builder)
//		else
//			LLVMBuildRet(context.builder, value.compile(context))
//	}
}
