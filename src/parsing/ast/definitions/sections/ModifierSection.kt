package parsing.ast.definitions.sections

import parsing.ast.definitions.Modifier
import parsing.ast.definitions.ModifierList
import parsing.ast.general.Element
import source_structure.Position
import util.indent
import util.toLines
import java.lang.StringBuilder

class ModifierSection(private val modifierList: ModifierList, private val sections: List<Element>,
					  end: Position): DeclarationSection(modifierList.start, end) {

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