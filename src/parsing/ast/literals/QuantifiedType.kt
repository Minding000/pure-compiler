package parsing.ast.literals

import linter.Linter
import linter.elements.literals.QuantifiedType
import linter.scopes.Scope
import parsing.ast.general.TypeElement

class QuantifiedType(private val baseType: TypeElement, private val hasDynamicQuantity: Boolean,
					 private val isOptional: Boolean): TypeElement(baseType.start, baseType.end) {

	override fun concretize(linter: Linter, scope: Scope): QuantifiedType {
		return QuantifiedType(this, baseType.concretize(linter, scope), hasDynamicQuantity, isOptional)
	}

	override fun toString(): String {
		return "QuantifiedType { ${if(hasDynamicQuantity) "..." else ""}$baseType${if(isOptional) "?" else ""} }"
	}
}