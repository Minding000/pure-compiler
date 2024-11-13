package components.semantic_model.values

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.code_generation.llvm.models.values.NullLiteral as NullLiteralUnit

class NullLiteral(override val source: SyntaxTreeNode, scope: Scope): LiteralValue(source, scope) {

	constructor(parent: SemanticModel): this(parent.source, parent.scope) {
		(providedType as? LiteralType)?.determineTypes()
	}

	init {
		providedType = LiteralType(source, scope, SpecialType.NULL)
		addSemanticModels(providedType)
	}

	override fun toUnit() = NullLiteralUnit(this)

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
