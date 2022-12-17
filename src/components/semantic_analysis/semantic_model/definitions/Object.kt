package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.tokenizer.WordAtom
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Object(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, superType: Type,
			 val isNative: Boolean, val isMutable: Boolean): TypeDefinition(source, name, scope, superType) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE, WordAtom.IMMUTABLE)
	}

	init {
		scope.typeDefinition = this
	}

	override fun register(linter: Linter, parentScope: MutableScope) {
		parentScope.declareType(linter, this)
		val type = ObjectType(this)
		val valueDeclaration = if(parentScope is TypeScope)
			PropertyDeclaration(source, name, type)
		else
			LocalVariableDeclaration(source, name, type)
		parentScope.declareValue(linter, valueDeclaration)
		addUnits(valueDeclaration)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Object {
		return this
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		scope.ensureTrivialInitializers(linter)
	}
}
