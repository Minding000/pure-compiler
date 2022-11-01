package components.parsing.syntax_tree.definitions.sections

import linting.Linter
import components.parsing.syntax_tree.definitions.Modifier
import components.parsing.syntax_tree.general.MetaElement
import components.tokenizer.WordAtom
import source_structure.Position
import java.util.*

abstract class DeclarationSection(start: Position, end: Position): MetaElement(start, end) {
	open var parent: ModifierSection? = null

	open fun validate(linter: Linter, allowedModifierTypes: List<WordAtom>) {
		parent?.validate(linter, allowedModifierTypes)
	}

	open fun containsModifier(searchedModifierType: WordAtom): Boolean {
		return parent?.containsModifier(searchedModifierType) ?: false
	}

	fun getModifiers(): List<Modifier> {
		var modifiers = getOwnModifiers()
		parent?.let { parent ->
			modifiers = modifiers.plus(parent.getModifiers())
		}
		return modifiers
	}

	protected open fun getOwnModifiers(): List<Modifier> {
		return LinkedList()
	}
}
