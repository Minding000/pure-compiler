package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.literals.NumberLiteral

class NumberLiteral(override val source: NumberLiteral, val value: String): LiteralValue(source) {

	init {
		//TODO allow for floating point numbers
		val type = ObjectType(source, Linter.Literals.NUMBER)
		units.add(type)
		this.type = type
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.numberLiteralScope?.let { literalScope -> super.linkTypes(linter, literalScope) }
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return LLVMConstInt(LLVMInt32Type(), value.toLong(), LLVMIRCompiler.LLVM_NO)
//	}
}