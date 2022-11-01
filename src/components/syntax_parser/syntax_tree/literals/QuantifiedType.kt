package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.types.OptionalType as SemanticOptionalTypeModel
import components.semantic_analysis.semantic_model.types.Type as SemanticTypeModel
import components.semantic_analysis.semantic_model.types.VariableAmountType as SemanticVariableAmountTypeModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeElement

class QuantifiedType(private val baseType: TypeElement, private val hasDynamicQuantity: Boolean,
					 private val isOptional: Boolean): TypeElement(baseType.start, baseType.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticTypeModel {
		var type = baseType.concretize(linter, scope)
		if(isOptional)
			type = SemanticOptionalTypeModel(this, type)
		if(hasDynamicQuantity)
			type = SemanticVariableAmountTypeModel(this, type)
		return type
	}

	override fun toString(): String {
		return "QuantifiedType { ${if(hasDynamicQuantity) "..." else ""}$baseType${if(isOptional) "?" else ""} }"
	}
}
