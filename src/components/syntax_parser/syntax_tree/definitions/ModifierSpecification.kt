package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.context.Context
import components.tokenizer.WordAtom
import logger.issues.modifiers.DisallowedModifier
import logger.issues.modifiers.DuplicateModifier

interface ModifierSpecification {

	fun getModifiers(): List<Modifier>

	fun validate(context: Context, allowedModifiers: List<WordAtom> = emptyList()) {
		val uniqueModifiers = HashSet<String>()
		for(modifier in getModifiers()) {
			val name = modifier.getValue()
			if(!allowedModifiers.contains(modifier.type)) {
				context.addIssue(DisallowedModifier(modifier))
				continue
			}
			if(uniqueModifiers.contains(name)) {
				context.addIssue(DuplicateModifier(modifier))
				continue
			}
			uniqueModifiers.add(name)
		}
	}

	fun containsModifier(modifier: WordAtom): Boolean {
		for(presentModifier in getModifiers())
			if(presentModifier.type == modifier)
				return true
		return false
	}
}
