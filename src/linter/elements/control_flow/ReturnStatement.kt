package linter.elements.control_flow

import linter.Linter
import linter.elements.values.Value
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.control_flow.ReturnStatement

class ReturnStatement(override val source: ReturnStatement, val value: Value?): Value(source) {

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
				linter.messages.add(Message("${source.getStartString()}: Failed to resolve type of value '${source.getValue()}'.",
					Message.Type.ERROR))
		}
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return if(value == null)
//			LLVMBuildRetVoid(context.builder)
//		else
//			LLVMBuildRet(context.builder, value.compile(context))
//	}
}