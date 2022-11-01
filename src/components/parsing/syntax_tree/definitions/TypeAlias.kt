package components.parsing.syntax_tree.definitions

import components.linting.Linter
import components.linting.semantic_model.definitions.TypeAlias as SemanticTypeAliasModel
import components.linting.semantic_model.scopes.MutableScope
import components.linting.semantic_model.scopes.TypeScope
import components.parsing.syntax_tree.definitions.sections.ModifierSection
import components.parsing.syntax_tree.definitions.sections.ModifierSectionChild
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.literals.Identifier
import components.parsing.syntax_tree.general.TypeElement
import source_structure.Position

class TypeAlias(start: Position, private val modifierList: ModifierList?, private val identifier: Identifier,
				private val type: TypeElement
): Element(start, type.end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun concretize(linter: Linter, scope: MutableScope): SemanticTypeAliasModel {
		modifierList?.validate(linter)
		val type = type.concretize(linter, scope)
		val typeScope = TypeScope(scope, null)
		val typeAlias = SemanticTypeAliasModel(this, identifier.getValue(), type, typeScope)
		typeScope.typeDefinition = typeAlias
		scope.declareType(linter, typeAlias)
		return typeAlias
	}

	override fun toString(): String {
		return "TypeAlias [ ${if(modifierList == null) "" else "$modifierList "}$identifier ] { $type }"
	}
}
