package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.general.Element

class BooleanLiteral(override val source: Element, scope: Scope, val value: Boolean): LiteralValue(source, scope) {

	constructor(source: Element, scope: Scope, value: Boolean, linter: Linter): this(source, scope, value) {
		(type as? LiteralType)?.determineTypes(linter)
	}

	init {
		type = LiteralType(source, scope, Linter.SpecialType.BOOLEAN)
		addUnits(type)
	}

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + value.hashCode()
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is BooleanLiteral)
			return false
		return value == other.value
	}

	override fun toString(): String {
		return if(value) "yes" else "no"
	}
}
