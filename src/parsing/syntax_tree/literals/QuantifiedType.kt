package parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.literals.OptionalType
import linting.semantic_model.literals.Type
import linting.semantic_model.literals.VariableAmountType
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.TypeElement

class QuantifiedType(private val baseType: TypeElement, private val hasDynamicQuantity: Boolean,
					 private val isOptional: Boolean): TypeElement(baseType.start, baseType.end) {

	override fun concretize(linter: Linter, scope: MutableScope): Type {
		var type = baseType.concretize(linter, scope)
		if(isOptional)
			type = OptionalType(this, type)
		if(hasDynamicQuantity)
			type = VariableAmountType(this, type)
		return type
	}

	override fun toString(): String {
		return "QuantifiedType { ${if(hasDynamicQuantity) "..." else ""}$baseType${if(isOptional) "?" else ""} }"
	}
}