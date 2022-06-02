package linter.elements.values

import compiler.targets.llvm.BuildContext
import compiler.targets.llvm.LLVMIRCompiler
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import parsing.ast.literals.NumberLiteral

class NumberLiteral(val source: NumberLiteral, val value: String): LiteralValue() {

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return LLVMConstInt(LLVMInt32Type(), value.toLong(), LLVMIRCompiler.LLVM_NO)
//	}
}