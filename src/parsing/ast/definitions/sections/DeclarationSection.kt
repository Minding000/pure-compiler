package parsing.ast.definitions.sections

import linter.Linter
import parsing.ast.definitions.Modifier
import parsing.ast.general.MetaElement
import parsing.tokenizer.WordAtom
import source_structure.Position
import java.util.*

abstract class DeclarationSection(start: Position, end: Position): MetaElement(start, end) {
	open var parent: ModifierSection? = null

	open fun validate(linter: Linter, allowedModifierTypes: List<WordAtom> = listOf()) {
		parent?.validate(linter, allowedModifierTypes)
	}

	open fun containsModifier(searchedModifierType: WordAtom): Boolean {
		return parent?.containsModifier(searchedModifierType) ?: false
	}

	fun getModifiers(): List<Modifier> {
		var modifiers = getOwnModifiers()
		parent?.let {
			modifiers = modifiers.plus(it.getModifiers())
		}
		return modifiers
	}

	protected open fun getOwnModifiers(): List<Modifier> {
		return LinkedList()
	}
}