package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.general.Element

class BooleanLiteral(override val source: Element, val value: Boolean): LiteralValue(source) {

	init {
		val type = LiteralType(source, Linter.SpecialType.BOOLEAN)
		addUnits(type)
		this.type = type
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
