package parsing.ast.definitions.sections

import linter.Linter
import linter.messages.Message
import parsing.ast.definitions.Modifier
import parsing.ast.general.MetaElement
import source_structure.Position
import java.util.*
import kotlin.collections.HashSet

abstract class DeclarationSection(start: Position, end: Position): MetaElement(start, end) {
	open var parent: ModifierSection? = null

	fun getModifiers(linter: Linter): List<Modifier> {
		var modifiers = getOwnModifiers()
		parent?.let {
			modifiers = modifiers.plus(it.getModifiers(linter))
		}
		checkForDuplicates(linter, modifiers)
		return modifiers
	}

	protected open fun getOwnModifiers(): List<Modifier> {
		return LinkedList()
	}

	private fun checkForDuplicates(linter: Linter, modifiers: List<Modifier>) {
		val uniqueModifiers = HashSet<String>()
		for(modifier in modifiers) {
			val name = modifier.getValue()
			if(uniqueModifiers.contains(name)) {
				linter.messages.add(Message("Duplicate '$name' modifier."))
				continue
			}
			uniqueModifiers.add(name)
		}
	}
}