package linter.elements.values

import compiler.targets.llvm.BuildContext
import linter.elements.general.Unit
import org.bytedeco.llvm.LLVM.LLVMValueRef

abstract class Value: Unit() {

//	abstract override fun compile(context: BuildContext): LLVMValueRef
}