package linter.elements.values

import compiler.targets.llvm.BuildContext
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM
import parsing.ast.literals.NullLiteral

class NullLiteral(override val source: NullLiteral): LiteralValue(source) {

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return LLVM.LLVMConstNull(resolveType())
//	}
}