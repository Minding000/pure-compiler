package linter.elements.values

import compiler.targets.llvm.BuildContext
import linter.Linter
import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.messages.Message
import org.bytedeco.llvm.LLVM.LLVMValueRef

abstract class Value(var type: Type? = null): Unit() {

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(type == null)
			linter.messages.add(Message("Failed to resolve type of value '${this.javaClass.name}'.", Message.Type.ERROR))
	}

//	abstract override fun compile(context: BuildContext): LLVMValueRef
}