package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import logger.issues.returns.RedundantReturnValue
import logger.issues.returns.ReturnStatementMissingValue
import logger.issues.returns.ReturnStatementOutsideOfCallable
import logger.issues.returns.ReturnValueTypeMismatch
import components.syntax_parser.syntax_tree.control_flow.ReturnStatement as ReturnStatementSyntaxTree

class ReturnStatement(override val source: ReturnStatementSyntaxTree, scope: Scope, val value: Value?): SemanticModel(source, scope) {
	var targetFunction: FunctionImplementation? = null
	override val isInterruptingExecution = true

	init {
		addSemanticModels(value)
	}

	override fun determineTypes() {
		super.determineTypes()
		val surroundingFunction = scope.getSurroundingFunction()
		if(surroundingFunction == null) {
			context.addIssue(ReturnStatementOutsideOfCallable(source))
			return
		}
		targetFunction = surroundingFunction
		surroundingFunction.mightReturnValue = true
		val returnType = surroundingFunction.signature.returnType
		if(value == null) {
			if(!SpecialType.NOTHING.matches(returnType))
				context.addIssue(ReturnStatementMissingValue(source))
		} else {
			if(SpecialType.NOTHING.matches(returnType)) {
				context.addIssue(RedundantReturnValue(source))
			} else if(value.isAssignableTo(returnType)) {
				value.setInferredType(returnType)
			} else {
				context.addIssue(ReturnValueTypeMismatch(source))
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		value?.analyseDataFlow(tracker)
		tracker.registerReturnStatement()
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return if(value == null)
//			LLVMBuildRetVoid(context.builder)
//		else
//			LLVMBuildRet(context.builder, value.compile(context))
//	}
}
