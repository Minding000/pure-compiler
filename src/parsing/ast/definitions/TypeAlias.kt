package parsing.ast.definitions

import parsing.ast.general.MetaElement
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import source_structure.Position

class TypeAlias(start: Position, private val modifierList: ModifierList?, private val identifier: Identifier,
				private val type: Type): MetaElement(start, type.end) {

	override fun toString(): String {
		return "TypeAlias [ ${if(modifierList == null) "" else "$modifierList "}$identifier ] { $type }"
	}
}