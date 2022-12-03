package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import components.tokenizer.WordAtom
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Class(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, superType: Type?,
			val isAbstract: Boolean, val isNative: Boolean, val isMutable: Boolean):
	TypeDefinition(source, name, scope, superType) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.IMMUTABLE, WordAtom.NATIVE)
	}

	init {
		scope.typeDefinition = this
	}

	override fun register(linter: Linter, parentScope: MutableScope) {
		parentScope.declareType(linter, this)
		val valueDeclaration = VariableValueDeclaration(source, name, StaticType(this))
		parentScope.declareValue(linter, valueDeclaration)
		addUnits(valueDeclaration)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Class {
		val superType = superType?.withTypeSubstitutions(typeSubstitutions)
		return Class(source, name, scope.withTypeSubstitutions(typeSubstitutions, superType?.scope), superType,
			isAbstract, isNative, isMutable)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(!isAbstract)
			scope.ensureNoAbstractMembers(linter)
	}
}
