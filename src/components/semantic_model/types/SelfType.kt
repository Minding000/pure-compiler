package components.semantic_model.types

import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.declaration.InvalidSelfTypeLocation

class SelfType(source: SyntaxTreeNode, scope: Scope): Type(source, scope) {
	var typeDeclaration: TypeDeclaration? = null

	constructor(typeDeclaration: TypeDeclaration): this(typeDeclaration.source, typeDeclaration.scope) {
		this.typeDeclaration = typeDeclaration
	}

	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): SelfType = this
	override fun simplified(): SelfType = this

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		return typeDeclaration?.scope?.getDirectTypeDeclaration(name)
	}

	override fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?> {
		return typeDeclaration?.scope?.getDirectValueDeclaration(name) ?: return Pair(null, null)
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		if(unresolvedSourceType is StaticType)
			return false
		val sourceType = unresolvedSourceType.effectiveType
		if(sourceType is ObjectType) //TODO also accept sub-types
			return sourceType.getTypeDeclaration() == typeDeclaration
		return sourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		if(targetType is ObjectType) //TODO also allow assignment to super-types
			return targetType.getTypeDeclaration() == typeDeclaration
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
}
