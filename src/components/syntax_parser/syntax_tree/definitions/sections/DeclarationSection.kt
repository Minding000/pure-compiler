package components.syntax_parser.syntax_tree.definitions.sections

import components.syntax_parser.syntax_tree.definitions.Modifier
import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode
import components.tokenizer.WordAtom
import source_structure.Position

abstract class DeclarationSection(start: Position, end: Position): MetaSyntaxTreeNode(start, end) {
	open var parent: ModifierSection? = null

	open fun validate(allowedModifiers: List<WordAtom>) {
		parent?.validate(allowedModifiers)
	}

	open fun containsModifier(modifier: WordAtom): Boolean {
		return parent?.containsModifier(modifier) ?: false
	}

	fun getModifiers(): List<Modifier> {
		val ownModifiers = getOwnModifiers()
		return parent?.getModifiers()?.plus(ownModifiers) ?: ownModifiers
	}

	protected open fun getOwnModifiers(): List<Modifier> = emptyList()
}
