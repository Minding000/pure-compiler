package components.semantic_analysis.semantic_model.values

import components.compiler.targets.llvm.Llvm
import components.compiler.targets.llvm.LlvmContext
import components.compiler.targets.llvm.LlvmValueReference
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import util.isRepresentedAsAnInteger
import java.math.BigDecimal

class NumberLiteral(override val source: SyntaxTreeNode, scope: Scope, val value: BigDecimal): LiteralValue(source, scope) {
	var isInteger = value.isRepresentedAsAnInteger()

	constructor(parent: SemanticModel, value: BigDecimal): this(parent.source, parent.scope, value) {
		(type as? LiteralType)?.determineTypes()
	}

	init {
		type = LiteralType(source, scope, if(isInteger) SpecialType.INTEGER else SpecialType.FLOAT)
		addSemanticModels(type)
	}

	override fun isAssignableTo(targetType: Type?): Boolean {
		if(targetType == null)
			return false
		return (type?.isAssignableTo(targetType) ?: false) || SpecialType.FLOAT.matches(targetType)
	}

	override fun setInferredType(inferredType: Type?) {
		val inferredBaseType = if(inferredType is OptionalType)
			inferredType.baseType
		else
			inferredType
		if(SpecialType.FLOAT.matches(inferredBaseType)) {
			type = inferredBaseType
			isInteger = false
		}
	}

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

	override fun getLlvmReference(llvmContext: LlvmContext): LlvmValueReference {
		return Llvm.buildInt32(llvmContext, value.longValueExact())
	}

	override fun toString(): String {
		return value.toString()
	}
}
