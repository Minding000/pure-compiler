package linting.semantic_model.control_flow

import linting.Linter
import linting.semantic_model.values.Value
import messages.Message
import linting.semantic_model.scopes.Scope
import components.parsing.syntax_tree.control_flow.ReturnStatement as ReturnStatementSyntaxTree

class ReturnStatement(override val source: ReturnStatementSyntaxTree, val value: Value?): Value(source) {
	override val isInterruptingExecution = true

	init {
		if(value != null)
			units.add(value)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		type = value?.type
	}

	override fun validate(linter: Linter) {
		if(value != null) {
			value.validate(linter)
			if(type == null)
				linter.addMessage(source, "Failed to resolve type of value '${source.getValue()}'.",
					Message.Type.ERROR)
		}
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return if(value == null)
//			LLVMBuildRetVoid(context.builder)
//		else
//			LLVMBuildRet(context.builder, value.compile(context))
//	}
}
