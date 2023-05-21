package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.definitions.TypeAlias as TypeAliasSyntaxTree

class TypeAlias(override val source: TypeAliasSyntaxTree, name: String, val referenceType: Type, scope: TypeScope,
				isSpecificCopy: Boolean = false):
	TypeDefinition(source, name, scope, null, null, emptyList(), false, isSpecificCopy) {
	override val isDefinition = false
	private var hasDeterminedEffectiveType = false
	private var effectiveType = referenceType

	init {
		scope.typeDefinition = this
		addSemanticModels(referenceType)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): TypeAlias {
		return TypeAlias(source, name, referenceType.withTypeSubstitutions(typeSubstitutions),
			scope.withTypeSubstitutions(typeSubstitutions, null), true)
	}

	fun getEffectiveType(): Type {
		if(!context.declarationStack.push(this))
			return effectiveType
		if(!hasDeterminedEffectiveType) {
			hasDeterminedEffectiveType = true
			referenceType.determineTypes()
			if(referenceType is ObjectType) {
				val referenceDefinition = referenceType.definition
				if(referenceDefinition is TypeAlias)
					effectiveType = referenceDefinition.getEffectiveType()
			}
		}
		context.declarationStack.pop(this)
		return effectiveType
	}

	override fun declare() {
		super.declare()
		scope.enclosingScope.declareType(this)
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return referenceType.getConversionsFrom(sourceType)
	}
}
