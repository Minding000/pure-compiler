package linter.elements.literals

import compiler.targets.llvm.BuildContext
import linter.elements.general.Unit
import linter.scopes.InterfaceScope
import org.bytedeco.llvm.LLVM.LLVMTypeRef

abstract class Type: Unit() {
	val scope = InterfaceScope()
	var llvmType: LLVMTypeRef? = null

	abstract fun accepts(sourceType: Type): Boolean
	abstract fun isAssignableTo(targetType: Type): Boolean

//	abstract override fun compile(context: BuildContext): LLVMTypeRef
}