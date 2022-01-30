package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.general.StatementBlock
import source_structure.Position
import util.indent
import util.toLines

class InitializerDefinition(start: Position, val modifierList: ModifierList?, val parameterList: ParameterList, val body: StatementBlock?): Element(start, body?.end ?: parameterList.end) {

	override fun toString(): String {
		return "Initializer [${if(modifierList == null) "" else "$modifierList "}$parameterList] { ${body ?: ""} }"
	}
}