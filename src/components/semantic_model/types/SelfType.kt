package components.semantic_model.types

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmType
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.declaration.InvalidSelfTypeLocation

class SelfType(source: SyntaxTreeNode, scope: Scope): Type(source, scope) {
	var typeDeclaration: TypeDeclaration? = null

	constructor(typeDeclaration: TypeDeclaration): this(typeDeclaration.source, typeDeclaration.scope) {
		this.typeDeclaration = typeDeclaration
	}

	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): SelfType = this
	override fun simplified(): SelfType = this

	override fun getLocalType(value: Value, sourceType: Type): Type {
		if(value.scope.getSurroundingTypeDeclaration() == typeDeclaration)
			return this
		if(sourceType is StaticType)
			return ObjectType(sourceType.typeDeclaration)
		return sourceType
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		return typeDeclaration?.scope?.getDirectTypeDeclaration(name)
	}

	override fun getValueDeclaration(name: String): ValueDeclaration.Match? {
		return typeDeclaration?.scope?.getDirectValueDeclaration(name)
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return typeDeclaration?.getConversionsFrom(sourceType) ?: emptyList()
	}

	override fun isInstanceOf(specialType: SpecialType): Boolean {
		return typeDeclaration?.getLinkedSuperType()?.isInstanceOf(specialType) ?: false
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		if(unresolvedSourceType is StaticType)
			return false
		val sourceType = unresolvedSourceType.effectiveType
		if(sourceType is ObjectType) {
			val sourceTypeDeclaration = sourceType.getTypeDeclaration()
			if(sourceTypeDeclaration == typeDeclaration)
				return true
			val sourceSuperType = sourceTypeDeclaration?.getLinkedSuperType() ?: return false
			return accepts(sourceSuperType)
		}
		return sourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		if(targetType is ObjectType) {
			if(targetType.getTypeDeclaration() == typeDeclaration)
				return true
			val superType = typeDeclaration?.getLinkedSuperType() ?: return false
			return targetType.accepts(superType)
		}
		return targetType is SelfType
	}

	override fun resolveTypeDeclarations() {
		if(typeDeclaration != null)
			return
		typeDeclaration = scope.getSurroundingTypeDeclaration()
		if(typeDeclaration == null)
			context.addIssue(InvalidSelfTypeLocation(source))
	}

	override fun toString() = "Self"

	override fun isLlvmPrimitive(): Boolean {
		val typeDeclaration = typeDeclaration ?: return false
		if(!typeDeclaration.isLlvmPrimitive())
			return false
		return typeDeclaration != context.primitiveCompilationTarget
	}

	override fun createLlvmType(constructor: LlvmConstructor): LlvmType {
		val typeDeclaration = typeDeclaration
		if(typeDeclaration != null && isLlvmPrimitive())
			return typeDeclaration.getLlvmReferenceType(constructor)
		return constructor.pointerType
	}
}
