package parsing.syntax_tree.definitions.sections

import errors.internal.CompilerError
import linting.Linter
import linting.semantic_model.general.Unit
import messages.Message
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.definitions.Modifier
import parsing.syntax_tree.definitions.ModifierList
import parsing.syntax_tree.general.Element
import parsing.tokenizer.WordAtom
import source_structure.Position
import util.indent
import util.toLines
import java.lang.StringBuilder

class ModifierSection(private val modifierList: ModifierList, private val sections: List<Element>,
					  end: Position): DeclarationSection(modifierList.start, end) {

	init {
		for(section in sections) {
			if(section !is ModifierSectionChild)
				throw CompilerError("Element not allowed in modifier section: $section")
			section.parent = this
		}
	}

	override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		for(section in sections)
			section.concretize(linter, scope, units)
	}

	override fun getOwnModifiers(): List<Modifier> {
		return modifierList.modifiers
	}

	override fun validate(linter: Linter, allowedModifierTypes: List<WordAtom>) { //TODO deduplicate validate() and contains() / ModifierList
		val uniqueModifiers = HashSet<String>()
		for(modifier in getModifiers()) {
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

	override fun containsModifier(searchedModifierType: WordAtom): Boolean {
		for(presentModifier in getModifiers())
			if(presentModifier.type == searchedModifierType)
				return true
		return false
	}

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("ModifierSection [ ")
			.append(modifierList)
			.append(" ] {")
			.append(sections.toLines().indent())
			.append("\n}")
		return string.toString()
	}
}