package components.parsing.syntax_tree.definitions

import components.parsing.syntax_tree.general.MetaElement

class ModifierList(private val modifiers: List<Modifier>):
	MetaElement(modifiers.first().start, modifiers.last().end), ModifierSpecification {

	override fun getModifiers(): List<Modifier> {
		return modifiers
	}

	override fun toString(): String {
		return "ModifierList { ${modifiers.joinToString(" ")} }"
	}
}
