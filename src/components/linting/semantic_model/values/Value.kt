package components.linting.semantic_model.values

import components.linting.Linter
import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.types.OptionalType
import components.linting.semantic_model.types.Type
import messages.Message
import components.syntax_parser.syntax_tree.general.Element

abstract class Value(override val source: Element, var type: Type? = null): Unit(source) {
	open var staticValue: Value? = null

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
		return type.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		if(other !is Value)
			return false
		return type == other.type
	}

//	abstract override fun compile(context: BuildContext): LLVMValueRef
}
