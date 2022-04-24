package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.TypeAlias
import linter.scopes.Scope
import parsing.ast.definitions.sections.ModifierSection
import parsing.ast.definitions.sections.ModifierSectionChild
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import source_structure.Position

class TypeAlias(start: Position, private val modifierList: ModifierList?, private val identifier: Identifier,
				private val type: Type): Element(start, type.end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun concretize(linter: Linter, scope: Scope): TypeAlias {
		val typeAlias = TypeAlias(this, identifier.getValue(), type.concretize(linter, scope))
		scope.declareType(linter, typeAlias)
		return typeAlias
	}

	override fun toString(): String {
		return "TypeAlias [ ${if(modifierList == null) "" else "$modifierList "}$identifier ] { $type }"
	}
}