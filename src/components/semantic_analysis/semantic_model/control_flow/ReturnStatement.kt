package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import messages.Message
import components.syntax_parser.syntax_tree.control_flow.ReturnStatement as ReturnStatementSyntaxTree

class ReturnStatement(override val source: ReturnStatementSyntaxTree, val value: Value?): Value(source) {
	var targetFunction: FunctionImplementation? = null
	override val isInterruptingExecution = true

	init {
		addUnits(value)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		type = value?.type
		val surroundingFunction = scope.getSurroundingFunction()
		if(surroundingFunction == null) {
			linter.addMessage(source, "Return statements are not allowed outside of functions.", Message.Type.ERROR)
		} else {
			targetFunction = surroundingFunction
			surroundingFunction.mightReturnValue = true
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		value?.analyseDataFlow(linter, tracker)
		tracker.registerExecutionEnd()
	}

	override fun validate(linter: Linter) {
		if(value != null) {
			value.validate(linter)
			if(type == null)
				linter.addMessage(source, "Failed to resolve type of value '${source.getValue()}'.", Message.Type.ERROR)
		}
		targetFunction?.signature?.returnType?.let { returnType ->
			if(Linter.SpecialType.NOTHING.matches(returnType)) {
				if(value != null)
					linter.addMessage(source, "Return value doesn't match the declared return type.", Message.Type.ERROR)
			} else {
				if(value?.isAssignableTo(returnType) != true)
					linter.addMessage(source, "Return value doesn't match the declared return type.", Message.Type.ERROR)
			}
		}
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return if(value == null)
//			LLVMBuildRetVoid(context.builder)
//		else
//			LLVMBuildRet(context.builder, value.compile(context))
//	}
}
