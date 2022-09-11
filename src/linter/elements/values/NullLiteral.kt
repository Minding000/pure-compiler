package linter.elements.values

import linter.Linter
import linter.elements.literals.ObjectType
import linter.scopes.Scope
import parsing.ast.literals.NullLiteral

class NullLiteral(override val source: NullLiteral): LiteralValue(source) {

	init {
		val type = ObjectType(source, Linter.Literals.NULL)
		this.type = type
		units.add(type)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.nullLiteralScope?.let { super.linkTypes(linter, it) }
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		return LLVM.LLVMConstNull(resolveType())
//	}
}