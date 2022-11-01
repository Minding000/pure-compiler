package components.linting.semantic_model.values

import components.linting.Linter
import components.linting.semantic_model.types.ObjectType
import components.linting.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element

class NullLiteral(override val source: Element): LiteralValue(source) {

	init {
		val nullType = ObjectType(source, Linter.LiteralType.NULL.className)
		units.add(nullType)
		type = nullType
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.link(Linter.LiteralType.NULL, type)
	}

	override fun hashCode(): Int {
		return NullLiteral::class.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		return other is NullLiteral
	}

	//	override fun compile(context: BuildContext): LLVMValueRef {
//		return LLVM.LLVMConstNull(resolveType())
//	}
}
