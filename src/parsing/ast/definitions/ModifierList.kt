package parsing.ast.definitions

import linter.Linter
import linter.messages.Message
import parsing.ast.general.MetaElement

class ModifierList(val modifiers: List<Modifier>): MetaElement(modifiers.first().start, modifiers.last().end) {

	fun checkForDuplicates(linter: Linter) {
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

	override fun toString(): String {
		return "ModifierList { ${modifiers.joinToString(" ")} }"
	}
}