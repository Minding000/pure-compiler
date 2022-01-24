package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.general.StatementBlock
import source_structure.Position
import util.indent
import util.toLines

class InitializerDefinition(start: Position, end: Position, val modifierList: ModifierList?, val parameters: List<Element>, val body: StatementBlock?): Element(start, end) {

	override fun toString(): String {
		return "Initializer [${if(modifierList == null) "" else "$modifierList "}${parameters.toLines().indent()}\n] { ${body ?: ""} }"
	}
}