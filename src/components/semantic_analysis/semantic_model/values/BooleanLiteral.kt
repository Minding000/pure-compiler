package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element

class BooleanLiteral(override val source: Element, val value: Boolean): LiteralValue(source) {

	init {
		val type = ObjectType(source, Linter.LiteralType.BOOLEAN.className)
		units.add(type)
		this.type = type
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.link(Linter.LiteralType.BOOLEAN, type)
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
}
