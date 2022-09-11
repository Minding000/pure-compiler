package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.TypeAlias
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.scopes.TypeScope
import parsing.syntax_tree.definitions.sections.ModifierSection
import parsing.syntax_tree.definitions.sections.ModifierSectionChild
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.literals.Identifier
import parsing.syntax_tree.general.TypeElement
import source_structure.Position

class TypeAlias(start: Position, private val modifierList: ModifierList?, private val identifier: Identifier,
				private val type: TypeElement
): Element(start, type.end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun concretize(linter: Linter, scope: MutableScope): TypeAlias {
		modifierList?.validate(linter)
		val type = type.concretize(linter, scope)
		val typeScope = TypeScope(scope, null)
		val typeAlias = TypeAlias(this, identifier.getValue(), type, typeScope)
		typeScope.typeDefinition = typeAlias
		scope.declareType(linter, typeAlias)
		return typeAlias
	}

	override fun toString(): String {
		return "TypeAlias [ ${if(modifierList == null) "" else "$modifierList "}$identifier ] { $type }"
	}
}