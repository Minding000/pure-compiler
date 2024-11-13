package components.semantic_model.values

import components.semantic_model.context.SpecialType
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.LiteralType
import components.semantic_model.types.OptionalType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import util.isRepresentedAsAnInteger
import java.math.BigDecimal
import components.code_generation.llvm.models.values.NumberLiteral as NumberLiteralUnit

class NumberLiteral(override val source: SyntaxTreeNode, scope: Scope, val value: BigDecimal): LiteralValue(source, scope) {

	constructor(parent: SemanticModel, value: BigDecimal): this(parent.source, parent.scope, value) {
		(providedType as? LiteralType)?.determineTypes()
	}

	init {
		providedType = LiteralType(source, scope, if(value.isRepresentedAsAnInteger()) SpecialType.INTEGER else SpecialType.FLOAT)
		addSemanticModels(providedType)
	}

	override fun isAssignableTo(targetType: Type?): Boolean {
		if(targetType == null)
			return false
		return (providedType?.isAssignableTo(targetType) ?: false) || SpecialType.BYTE.matches(targetType) || SpecialType.FLOAT.matches(
			targetType)
	}

	override fun setInferredType(inferredType: Type?) {
		val inferredBaseType = if(inferredType is OptionalType) inferredType.baseType else inferredType
		if(SpecialType.BYTE.matches(inferredBaseType) || SpecialType.FLOAT.matches(inferredBaseType))
			providedType = inferredBaseType
	}

	override fun toUnit() = NumberLiteralUnit(this)

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + value.hashCode()
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is NumberLiteral)
			return false
		return value == other.value
	}

	override fun toString(): String {
		return value.toString()
	}
}
