package linter.elements.values

import compiler.targets.llvm.BuildContext
import linter.Linter
import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.messages.Message
import org.bytedeco.llvm.LLVM.LLVMValueRef
import parsing.ast.general.Element

abstract class Value(open val source: Element, var type: Type? = null): Unit() {

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(type == null)
			linter.messages.add(Message("${source.getStartString()}: Failed to resolve type of value '${source.getValue()}'.", Message.Type.ERROR))
	}

//	abstract override fun compile(context: BuildContext): LLVMValueRef
}