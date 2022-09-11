package parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.literals.QuantifiedType
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.TypeElement

class QuantifiedType(private val baseType: TypeElement, private val hasDynamicQuantity: Boolean,
					 private val isOptional: Boolean): TypeElement(baseType.start, baseType.end) {

	override fun concretize(linter: Linter, scope: MutableScope): QuantifiedType {
		return QuantifiedType(this, baseType.concretize(linter, scope), hasDynamicQuantity, isOptional)
	}

	override fun toString(): String {
		return "QuantifiedType { ${if(hasDynamicQuantity) "..." else ""}$baseType${if(isOptional) "?" else ""} }"
	}
}