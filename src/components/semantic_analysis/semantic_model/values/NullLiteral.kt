package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.general.Element

class NullLiteral(override val source: Element, scope: Scope): LiteralValue(source, scope) {

	constructor(parent: SemanticModel): this(parent.source, parent.scope) {
		(type as? LiteralType)?.determineTypes()
	}

	init {
		type = LiteralType(source, scope, SpecialType.NULL)
		addSemanticModels(type)
	}

	override fun hashCode(): Int {
		return NullLiteral::class.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		return other is NullLiteral
	}

	override fun toString(): String {
		return "null"
	}

	//	override fun compile(context: BuildContext): LLVMValueRef {
//		return LLVM.LLVMConstNull(resolveType())
//	}
}
