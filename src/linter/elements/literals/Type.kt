package linter.elements.literals

import compiler.targets.llvm.BuildContext
import linter.Linter
import linter.elements.general.Unit
import linter.messages.Message
import linter.scopes.InterfaceScope
import org.bytedeco.llvm.LLVM.LLVMTypeRef

abstract class Type: Unit() {
	val scope = InterfaceScope()
	var llvmType: LLVMTypeRef? = null

	abstract fun accepts(sourceType: Type): Boolean
	abstract fun isAssignableTo(targetType: Type): Boolean

	open fun getKeyType(linter: Linter): Type? {
		linter.messages.add(Message("Type '$this' doesn't have a key type.", Message.Type.ERROR))
		return null
	}

	open fun getValueType(linter: Linter): Type? {
		linter.messages.add(Message("Type '$this' doesn't have a value type.", Message.Type.ERROR))
		return null
	}

//	abstract override fun compile(context: BuildContext): LLVMTypeRef
}