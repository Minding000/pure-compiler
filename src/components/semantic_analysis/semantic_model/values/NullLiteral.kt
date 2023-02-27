package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.general.Element

class NullLiteral(override val source: Element, scope: Scope): LiteralValue(source, scope) {

	constructor(source: Element, scope: Scope, linter: Linter): this(source, scope) {
		(type as? LiteralType)?.linkTypes(linter)
	}

	init {
		type = LiteralType(source, scope, Linter.SpecialType.NULL)
		addUnits(type)
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
