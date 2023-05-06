package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
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
		addUnits(referenceType)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): TypeAlias {
		return TypeAlias(source, name, referenceType.withTypeSubstitutions(linter, typeSubstitutions),
			scope.withTypeSubstitutions(linter, typeSubstitutions, null), true)
	}

	fun getEffectiveType(linter: Linter): Type {
		if(!linter.declarationStack.push(this))
			return effectiveType
		if(hasDeterminedEffectiveType)
			return effectiveType
		hasDeterminedEffectiveType = true
		referenceType.determineTypes(linter)
		if(referenceType is ObjectType) {
			val referenceDefinition = referenceType.definition
			if(referenceDefinition is TypeAlias)
				effectiveType = referenceDefinition.getEffectiveType(linter)
		}
		linter.declarationStack.pop(this)
		return effectiveType
	}

	override fun declare(linter: Linter) {
		super.declare(linter)
		scope.enclosingScope.declareType(linter, this)
	}

	override fun getConversionsFrom(linter: Linter, sourceType: Type): List<InitializerDefinition> {
		return referenceType.getConversionsFrom(linter, sourceType)
	}
}
