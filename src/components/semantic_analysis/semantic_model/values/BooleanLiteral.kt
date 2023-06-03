package components.semantic_analysis.semantic_model.values

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

class BooleanLiteral(override val source: SyntaxTreeNode, scope: Scope, val value: Boolean): LiteralValue(source, scope) {

	constructor(parent: SemanticModel, value: Boolean): this(parent.source, parent.scope, value) {
		(type as? LiteralType)?.determineTypes()
	}

	init {
		type = LiteralType(source, scope, SpecialType.BOOLEAN)
		addSemanticModels(type)
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

	override fun getLlvmReference(constructor: LlvmConstructor): LlvmValue {
		return constructor.buildBoolean(value)
	}

	override fun toString(): String {
		return if(value) "yes" else "no"
	}
}
