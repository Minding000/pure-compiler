package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.literals.OptionalType
import linting.semantic_model.literals.Type
import messages.Message
import parsing.syntax_tree.general.Element

abstract class Value(open val source: Element, var type: Type? = null): Unit() {

	open fun isAssignableTo(targetType: Type?): Boolean {
		return type?.let { type -> targetType?.accepts(type) } ?: false
	}

	fun setInferredType(inferredType: Type?) {
		if(type == null) {
			type = if(inferredType is OptionalType)
				inferredType.baseType
			else
				inferredType
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(type == null)
			linter.addMessage(source, "Failed to resolve type of value '${source.getValue()}'.",
				Message.Type.ERROR)
	}

	override fun hashCode(): Int {
		return type?.hashCode() ?: 0
	}

	override fun equals(other: Any?): Boolean {
		if(other !is Value)
			return false
		return type == other.type
	}

//	abstract override fun compile(context: BuildContext): LLVMValueRef
}
