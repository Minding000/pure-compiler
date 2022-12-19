package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import util.isRepresentedAsAnInteger
import java.math.BigDecimal

class NumberLiteral(override val source: Element, val value: BigDecimal): LiteralValue(source) {
	var isInteger = value.isRepresentedAsAnInteger()
	var literalType = if(isInteger) Linter.LiteralType.INTEGER else Linter.LiteralType.FLOAT

	init {
		val numberType = ObjectType(source, literalType.className)
		addUnits(numberType)
		type = numberType
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.link(literalType, type)
	}

	override fun isAssignableTo(targetType: Type?): Boolean {
		return Linter.LiteralType.INTEGER.matches(targetType) || Linter.LiteralType.FLOAT.matches(targetType)
	}

	override fun setInferredType(inferredType: Type?) {
		val inferredBaseType = if(inferredType is OptionalType)
			inferredType.baseType
		else
			inferredType
		if(Linter.LiteralType.FLOAT.matches(inferredBaseType)) {
			type = inferredBaseType
			isInteger = false
			literalType = Linter.LiteralType.FLOAT
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

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return LLVMConstInt(LLVMInt32Type(), value.toLong(), LLVMIRCompiler.LLVM_NO)
//	}
}
