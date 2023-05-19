package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.semantic_analysis.semantic_model.types.OptionalType as SemanticOptionalTypeModel
import components.semantic_analysis.semantic_model.types.PluralType as SemanticPluralTypeModel
import components.semantic_analysis.semantic_model.types.Type as SemanticTypeModel

class QuantifiedType(private val baseType: TypeSyntaxTreeNode, private val hasDynamicQuantity: Boolean, private val isOptional: Boolean):
	TypeSyntaxTreeNode(baseType.start, baseType.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticTypeModel {
		var type = baseType.toSemanticModel(scope)
		if(isOptional)
			type = SemanticOptionalTypeModel(this, scope, type)
		if(hasDynamicQuantity)
			type = SemanticPluralTypeModel(this, scope, type)
		return type
	}

	override fun toString(): String {
		return "QuantifiedType { ${if(hasDynamicQuantity) "..." else ""}$baseType${if(isOptional) "?" else ""} }"
	}
}
