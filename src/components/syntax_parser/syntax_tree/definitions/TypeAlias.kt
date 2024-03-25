package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.semantic_model.scopes.TypeScope
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import util.indent
import util.toLines
import components.semantic_model.declarations.TypeAlias as SemanticTypeAliasModel

class TypeAlias(private val modifierList: ModifierList?, private val identifier: Identifier, private val type: TypeSyntaxTreeNode,
				private val instanceLists: List<InstanceList>, start: Position, end: Position):
	SyntaxTreeNode(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>) {
		val typeAlias = toSemanticModel(scope)
		semanticModels.add(typeAlias.getValueDeclaration())
		semanticModels.add(typeAlias)
	}

	override fun toSemanticModel(scope: MutableScope): SemanticTypeAliasModel {
		modifierList?.validate(context)
		val typeScope = TypeScope(scope)
		val type = type.toSemanticModel(scope)
		val instances = instanceLists.flatMap { instanceList -> instanceList.toSemanticInstanceModels(typeScope) }
		return SemanticTypeAliasModel(this, typeScope, identifier.getValue(), type, instances)
	}

	override fun toString(): String {
		var stringRepresentation = "TypeAlias [ ${if(modifierList == null) "" else "$modifierList "}$identifier = $type ]"
		if(instanceLists.isNotEmpty())
			stringRepresentation += " {${instanceLists.toLines().indent()}\n}"
		return stringRepresentation
	}
}
