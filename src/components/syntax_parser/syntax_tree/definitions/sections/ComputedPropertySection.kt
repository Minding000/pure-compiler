package components.syntax_parser.syntax_tree.definitions.sections

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.ComputedPropertyDeclaration
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines

class ComputedPropertySection(start: Position, end: Position, val type: TypeSyntaxTreeNode?,
							  private val computedProperties: List<ComputedPropertyDeclaration>):
	DeclarationSection(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	init {
		for(computedProperty in computedProperties)
			computedProperty.parent = this
	}

	override fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>) {
		for(computedProperty in computedProperties)
			computedProperty.toSemanticModel(scope, semanticModels)
	}

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("ComputedPropertySection [")
		if(type != null)
			string.append(" $type ")
		string
			.append("] {")
			.append(computedProperties.toLines().indent())
			.append("\n}")
		return string.toString()
	}
}
