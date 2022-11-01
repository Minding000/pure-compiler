package components.syntax_parser.syntax_tree.definitions

import components.linting.Linter
import messages.Message
import components.tokenizer.WordAtom

interface ModifierSpecification {

	fun getModifiers(): List<Modifier>

	fun validate(linter: Linter, allowedModifierTypes: List<WordAtom> = listOf()) {
		val uniqueModifiers = HashSet<String>()
		for(modifier in getModifiers()) {
			val name = modifier.getValue()
			if(!allowedModifierTypes.contains(modifier.type)) {
				linter.addMessage(modifier, "Modifier '$name' is not allowed here.", Message.Type.WARNING)
				continue
			}
			if(uniqueModifiers.contains(name)) {
				linter.addMessage(modifier, "Duplicate '$name' modifier.", Message.Type.WARNING)
				continue
			}
			uniqueModifiers.add(name)
		}
	}

	fun containsModifier(searchedModifierType: WordAtom): Boolean {
		for(presentModifier in getModifiers())
			if(presentModifier.type == searchedModifierType)
				return true
		return false
	}
}
