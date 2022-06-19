package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.TypeAlias
import linter.scopes.MutableScope
import linter.scopes.TypeScope
import parsing.ast.definitions.sections.ModifierSection
import parsing.ast.definitions.sections.ModifierSectionChild
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import parsing.ast.general.TypeElement
import source_structure.Position

class TypeAlias(start: Position, private val modifierList: ModifierList?, private val identifier: Identifier,
				private val type: TypeElement
): Element(start, type.end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun concretize(linter: Linter, scope: MutableScope): TypeAlias {
		modifierList?.validate(linter)
		val type = type.concretize(linter, scope)
		val typeScope = TypeScope(scope, type.scope)
		val typeAlias = TypeAlias(this, identifier.getValue(), type, typeScope)
		typeScope.typeDefinition = typeAlias
		scope.declareType(linter, typeAlias)
		return typeAlias
	}

	override fun toString(): String {
		return "TypeAlias [ ${if(modifierList == null) "" else "$modifierList "}$identifier ] { $type }"
	}
}