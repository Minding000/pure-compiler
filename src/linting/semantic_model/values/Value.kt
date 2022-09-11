package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.literals.Type
import linting.messages.Message
import parsing.syntax_tree.general.Element

abstract class Value(open val source: Element, var type: Type? = null): Unit() {

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(type == null)
			linter.messages.add(Message("${source.getStartString()}: " +
					"Failed to resolve type of value '${source.getValue()}'.", Message.Type.ERROR))
	}

//	abstract override fun compile(context: BuildContext): LLVMValueRef
}