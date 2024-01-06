package components.semantic_model.values

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

class NullLiteral(override val source: SyntaxTreeNode, scope: Scope): LiteralValue(source, scope) {

	constructor(parent: SemanticModel): this(parent.source, parent.scope) {
		(type as? LiteralType)?.determineTypes()
	}

	init {
		type = LiteralType(source, scope, SpecialType.NULL)
		addSemanticModels(type)
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return constructor.nullPointer
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
}
