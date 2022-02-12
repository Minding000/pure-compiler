package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.general.StatementSection
import source_structure.Position

class DeinitializerDefinition(start: Position, end: Position, val modifierList: ModifierList?, val body: StatementSection?): Element(start, end) {

	override fun toString(): String {
		return "Deinitializer [ ${if(modifierList == null) "" else "$modifierList "} ] { ${body ?: ""} }"
	}
}