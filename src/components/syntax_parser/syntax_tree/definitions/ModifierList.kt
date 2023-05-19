package components.syntax_parser.syntax_tree.definitions

import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode

class ModifierList(private val modifiers: List<Modifier>):
	MetaSyntaxTreeNode(modifiers.first().start, modifiers.last().end), ModifierSpecification {

	override fun getModifiers(): List<Modifier> {
		return modifiers
	}

	override fun toString(): String {
		return "ModifierList { ${modifiers.joinToString(" ")} }"
	}
}
