package components.semantic_model.values

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

class BooleanLiteral(override val source: SyntaxTreeNode, scope: Scope, val value: Boolean): LiteralValue(source, scope) {

	constructor(parent: SemanticModel, value: Boolean): this(parent.source, parent.scope, value) {
		(providedType as? LiteralType)?.determineTypes()
	}

	init {
		providedType = LiteralType(source, scope, SpecialType.BOOLEAN)
		addSemanticModels(providedType)
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return constructor.buildBoolean(value)
	}

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + value.hashCode()
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is BooleanLiteral)
			return false
		return value == other.value
	}

	override fun toString(): String {
		return if(value) "yes" else "no"
	}
}
