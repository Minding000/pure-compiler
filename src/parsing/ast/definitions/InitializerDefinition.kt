package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.general.StatementSection
import source_structure.Position

class InitializerDefinition(start: Position, val modifierList: ModifierList?, val parameterList: ParameterList, val body: StatementSection?): Element(start, body?.end ?: parameterList.end) {

	override fun toString(): String {
		return "Initializer [ ${if(modifierList == null) "" else "$modifierList "}$parameterList ] { ${body ?: ""} }"
	}
}