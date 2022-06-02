package linter.elements.control_flow

import compiler.targets.llvm.BuildContext
import linter.elements.values.Value
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import parsing.ast.control_flow.ReturnStatement

class ReturnStatement(val source: ReturnStatement, val value: Value?): Value() {

	init {
		if(value != null)
			units.add(value)
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return if(value == null)
//			LLVMBuildRetVoid(context.builder)
//		else
//			LLVMBuildRet(context.builder, value.compile(context))
//	}
}