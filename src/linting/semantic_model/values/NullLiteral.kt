package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.literals.NullLiteral

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