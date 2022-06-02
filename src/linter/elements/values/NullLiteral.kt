package linter.elements.values

import compiler.targets.llvm.BuildContext
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM
import parsing.ast.literals.NullLiteral

class NullLiteral(val source: NullLiteral): LiteralValue() {

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return LLVM.LLVMConstNull(resolveType())
//	}
}