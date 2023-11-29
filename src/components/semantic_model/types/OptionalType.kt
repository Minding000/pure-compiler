package components.semantic_model.types

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

class OptionalType(override val source: SyntaxTreeNode, scope: Scope, val baseType: Type): Type(source, scope) {

	init {
		addSemanticModels(baseType)
	}

	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): OptionalType {
		return OptionalType(source, scope, baseType.withTypeSubstitutions(typeSubstitutions))
	}

	override fun simplified(): Type {
		var baseType = baseType
		while(baseType is OptionalType)
			baseType = baseType.baseType
		return OptionalType(source, scope, baseType.simplified())
	}

	override fun getLocalType(value: Value, sourceType: Type): Type {
		return OptionalType(source, scope, baseType.getLocalType(value, sourceType))
	}

	override fun isMemberAccessible(signature: FunctionSignature, requireSpecificType: Boolean): Boolean {
		return baseType.isMemberAccessible(signature, requireSpecificType)
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		return null
	}

	override fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?> {
		return Pair(null, null)
	}

	override fun inferTypeParameter(typeParameter: TypeDeclaration, sourceType: Type, inferredTypes: MutableList<Type>) {
		val sourceBaseType = if(sourceType is OptionalType) sourceType.baseType else sourceType
		baseType.inferTypeParameter(typeParameter, sourceBaseType, inferredTypes)
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return baseType.getConversionsFrom(sourceType)
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		var sourceType = unresolvedSourceType.effectiveType
		if(SpecialType.NULL.matches(sourceType))
			return true
		if(sourceType is OptionalType)
			sourceType = sourceType.baseType
		return baseType.accepts(sourceType)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		if(targetType !is OptionalType)
			return false
		return baseType.isAssignableTo(targetType.baseType)
	}

	override fun equals(other: Any?): Boolean {
		if(other !is OptionalType)
			return false
		if(baseType != other.baseType)
			return false
		return true
	}

	override fun createLlvmType(constructor: LlvmConstructor): LlvmType {
		return constructor.pointerType
	}

	override fun hashCode(): Int {
		return baseType.hashCode()
	}

	override fun toString(): String {
		return "$baseType?"
	}
}
