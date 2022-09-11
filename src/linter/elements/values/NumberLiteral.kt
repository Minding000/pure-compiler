package linter.elements.values

import linter.Linter
import linter.elements.literals.ObjectType
import linter.scopes.Scope
import parsing.ast.literals.NumberLiteral

class NumberLiteral(override val source: NumberLiteral, val value: String): LiteralValue(source) {

	init {
		//TODO allow for floating point numbers
		val type = ObjectType(source, Linter.Literals.NUMBER)
		units.add(type)
		this.type = type
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.numberLiteralScope?.let { super.linkTypes(linter, it) }
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return LLVMConstInt(LLVMInt32Type(), value.toLong(), LLVMIRCompiler.LLVM_NO)
//	}
}