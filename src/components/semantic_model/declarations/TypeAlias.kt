package components.semantic_model.declarations

import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.definitions.TypeAlias as TypeAliasSyntaxTree

class TypeAlias(override val source: TypeAliasSyntaxTree, name: String, val referenceType: Type, scope: TypeScope):
	TypeDeclaration(source, name, scope, null, null, emptyList(), false) {
	override val isDefinition = false
	private var hasDeterminedEffectiveType = false
	private var effectiveType = referenceType

	init {
		scope.typeDeclaration = this
		addSemanticModels(referenceType)
	}

	fun getEffectiveType(): Type {
		if(!context.declarationStack.push(this))
			return effectiveType
		if(!hasDeterminedEffectiveType) {
			hasDeterminedEffectiveType = true
			referenceType.determineTypes()
			if(referenceType is ObjectType) {
				val referenceTypeDeclaration = referenceType.getTypeDeclaration()
				if(referenceTypeDeclaration is TypeAlias)
					effectiveType = referenceTypeDeclaration.getEffectiveType()
			}
		}
		context.declarationStack.pop(this)
		return effectiveType
	}

	override fun declare() {
		super.declare()
		scope.enclosingScope.addTypeDeclaration(this)
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return referenceType.getConversionsFrom(sourceType)
	}
}
