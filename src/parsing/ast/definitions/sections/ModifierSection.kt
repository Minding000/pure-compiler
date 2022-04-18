package parsing.ast.definitions.sections

import errors.internal.CompilerError
import linter.Linter
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.definitions.Modifier
import parsing.ast.definitions.ModifierList
import parsing.ast.general.Element
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

	override fun concretize(linter: Linter, scope: Scope, units: MutableList<Unit>) {
		for(section in sections)
			section.concretize(linter, scope, units)
	}

	override fun getOwnModifiers(): List<Modifier> {
		return modifierList.modifiers
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