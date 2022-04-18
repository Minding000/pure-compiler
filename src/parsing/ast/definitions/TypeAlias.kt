package parsing.ast.definitions

import parsing.ast.definitions.sections.ModifierSection
import parsing.ast.definitions.sections.ModifierSectionChild
import parsing.ast.general.MetaElement
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import source_structure.Position

class TypeAlias(start: Position, private val modifierList: ModifierList?, private val identifier: Identifier,
				private val type: Type): MetaElement(start, type.end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun toString(): String {
		return "TypeAlias [ ${if(modifierList == null) "" else "$modifierList "}$identifier ] { $type }"
	}
}