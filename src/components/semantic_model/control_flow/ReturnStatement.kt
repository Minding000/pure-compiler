package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import logger.issues.returns.RedundantReturnValue
import logger.issues.returns.ReturnStatementMissingValue
import logger.issues.returns.ReturnStatementOutsideOfCallable
import logger.issues.returns.ReturnValueTypeMismatch
import components.syntax_parser.syntax_tree.control_flow.ReturnStatement as ReturnStatementSyntaxTree

class ReturnStatement(override val source: ReturnStatementSyntaxTree, scope: Scope, val value: Value?): SemanticModel(source, scope) {
	override val isInterruptingExecution = true
	private var targetFunction: FunctionImplementation? = null

	init {
		addSemanticModels(value)
	}

	override fun determineTypes() {
		super.determineTypes()
		determineTargetFunction()
	}

	private fun determineTargetFunction() {
		val surroundingFunction = scope.getSurroundingFunction()
		if(surroundingFunction == null) {
			context.addIssue(ReturnStatementOutsideOfCallable(source))
			return
		}
		targetFunction = surroundingFunction
		surroundingFunction.mightReturnValue = true
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		value?.analyseDataFlow(tracker)
		tracker.registerReturnStatement()
	}

	override fun validate() {
		super.validate()
		validateReturnType()
	}

	private fun validateReturnType() {
		val returnType = targetFunction?.signature?.returnType ?: return
		if(value == null) {
			if(!SpecialType.NOTHING.matches(returnType))
				context.addIssue(ReturnStatementMissingValue(source))
		} else {
			if(SpecialType.NOTHING.matches(returnType)) {
				context.addIssue(RedundantReturnValue(source))
			} else if(value.isAssignableTo(returnType)) {
				value.setInferredType(returnType)
			} else {
				val valueType = value.type
				if(valueType != null)
					context.addIssue(ReturnValueTypeMismatch(source, valueType, returnType))
			}
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		constructor.buildReturn(value?.getLlvmValue(constructor))
	}
}
