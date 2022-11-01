package parsing.syntax_tree.definitions.sections

import errors.internal.CompilerError
import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.definitions.Modifier
import parsing.syntax_tree.definitions.ModifierList
import parsing.syntax_tree.definitions.ModifierSpecification
import parsing.syntax_tree.general.Element
import components.tokenizer.WordAtom
import source_structure.Position
import util.indent
import util.toLines

class ModifierSection(private val modifierList: ModifierList, private val sections: List<Element>,
					  end: Position): DeclarationSection(modifierList.start, end), ModifierSpecification {

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
		return modifierList.getModifiers()
	}

	override fun validate(linter: Linter, allowedModifierTypes: List<WordAtom>) {
		super<ModifierSpecification>.validate(linter, allowedModifierTypes)
	}

	override fun containsModifier(searchedModifierType: WordAtom): Boolean {
		return super<ModifierSpecification>.containsModifier(searchedModifierType)
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
