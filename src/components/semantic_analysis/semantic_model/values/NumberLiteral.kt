package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import util.isRepresentedAsAnInteger
import java.math.BigDecimal

class NumberLiteral(override val source: Element, scope: Scope, val value: BigDecimal): LiteralValue(source, scope) {
	var isInteger = value.isRepresentedAsAnInteger()

	constructor(source: Element, scope: Scope, value: BigDecimal, linter: Linter): this(source, scope, value) {
		(type as? LiteralType)?.determineTypes(linter)
	}

	init {
		type = LiteralType(source, scope, if(isInteger) Linter.SpecialType.INTEGER else Linter.SpecialType.FLOAT)
		addUnits(type)
	}

	override fun isAssignableTo(targetType: Type?): Boolean {
		if(targetType == null)
			return false
		return (type?.isAssignableTo(targetType) ?: false) || Linter.SpecialType.FLOAT.matches(targetType)
	}

	override fun setInferredType(inferredType: Type?) {
		val inferredBaseType = if(inferredType is OptionalType)
			inferredType.baseType
		else
			inferredType
		if(Linter.SpecialType.FLOAT.matches(inferredBaseType)) {
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

	override fun toString(): String {
		return value.toString()
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return LLVMConstInt(LLVMInt32Type(), value.toLong(), LLVMIRCompiler.LLVM_NO)
//	}
}
