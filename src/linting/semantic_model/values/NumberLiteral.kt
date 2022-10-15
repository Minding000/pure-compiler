package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.types.ObjectType
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.literals.NumberLiteral as NumberLiteralSyntaxLiteral

class NumberLiteral(override val source: NumberLiteralSyntaxLiteral, val value: String): LiteralValue(source) {

	init {
		//TODO allow for floating point numbers
		val numberType = ObjectType(source, Linter.LiteralType.NUMBER.className)
		units.add(numberType)
		type = numberType
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.link(Linter.LiteralType.NUMBER, type)
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
