package parsing.syntax_tree.definitions

import linting.Linter
import messages.Message
import parsing.syntax_tree.general.MetaElement
import parsing.tokenizer.WordAtom

class ModifierList(val modifiers: List<Modifier>): MetaElement(modifiers.first().start, modifiers.last().end) {

	fun validate(linter: Linter, allowedModifierTypes: List<WordAtom> = listOf()) {
		val uniqueModifiers = HashSet<String>()
		for(modifier in modifiers) {
			val name = modifier.getValue()
			if(!allowedModifierTypes.contains(modifier.type)) {
				linter.messages.add(Message("${modifier.getStartString()}: Modifier '$name' is not allowed here.", Message.Type.WARNING))
				continue
			}
			if(uniqueModifiers.contains(name)) {
				linter.messages.add(Message("${modifier.getStartString()}: Duplicate '$name' modifier.", Message.Type.WARNING))
				continue
			}
			uniqueModifiers.add(name)
		}
	}

	fun contains(searchedModifierType: WordAtom): Boolean {
		for(presentModifier in modifiers)
			if(presentModifier.type == searchedModifierType)
				return true
		return false
	}

	override fun toString(): String {
		return "ModifierList { ${modifiers.joinToString(" ")} }"
	}
}