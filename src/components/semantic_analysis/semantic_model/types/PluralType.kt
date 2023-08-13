package components.semantic_analysis.semantic_model.types

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

class PluralType(override val source: SyntaxTreeNode, scope: Scope, val baseType: Type): Type(source, scope) {

	init {
		addSemanticModels(baseType)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): PluralType {
		return PluralType(source, scope, baseType.withTypeSubstitutions(typeSubstitutions))
	}

	override fun simplified(): Type {
		return PluralType(source, scope, baseType.simplified())
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		val sourceType = unresolvedSourceType.effectiveType
		if(sourceType !is PluralType)
			return false
		return baseType.accepts(sourceType.baseType)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		if(targetType !is PluralType)
			return false
		return baseType.isAssignableTo(targetType.baseType)
	}

	override fun equals(other: Any?): Boolean {
		if(other !is PluralType)
			return false
		if(baseType != other.baseType)
			return false
		return true
	}

	override fun hashCode(): Int {
		return baseType.hashCode()
	}

	override fun toString(): String {
		return "...$baseType"
	}

	override fun createLlvmType(constructor: LlvmConstructor): LlvmType {
		return constructor.i32Type
	}
}
