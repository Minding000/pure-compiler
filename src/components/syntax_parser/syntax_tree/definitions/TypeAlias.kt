package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import components.semantic_analysis.semantic_model.definitions.TypeAlias as SemanticTypeAliasModel

class TypeAlias(start: Position, private val modifierList: ModifierList?, private val identifier: Identifier,
				private val type: TypeSyntaxTreeNode): SyntaxTreeNode(start, type.end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun toSemanticModel(scope: MutableScope): SemanticTypeAliasModel {
		modifierList?.validate(context)
		val type = type.toSemanticModel(scope)
		val typeScope = TypeScope(scope, null)
		return SemanticTypeAliasModel(this, identifier.getValue(), type, typeScope)
	}

	override fun toString(): String {
		return "TypeAlias [ ${if(modifierList == null) "" else "$modifierList "}$identifier ] { $type }"
	}
}
