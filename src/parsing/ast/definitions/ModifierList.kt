package parsing.ast.definitions

import parsing.ast.Element
import parsing.tokenizer.Word
import java.lang.StringBuilder

class ModifierList(val modifiers: List<Modifier>): Element(modifiers.first().start, modifiers.last().end) {

	override fun toString(): String {
		return "ModifierList { ${modifiers.joinToString(" ")} }"
	}
}