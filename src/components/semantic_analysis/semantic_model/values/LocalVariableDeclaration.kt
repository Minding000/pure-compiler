package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.literals.Identifier

class LocalVariableDeclaration(source: Element, scope: Scope, name: String, type: Type? = null, value: Value? = null,
							   isConstant: Boolean = true, isMutable: Boolean = false):
	ValueDeclaration(source, scope, name, type, value, isConstant, isMutable) {

	constructor(source: Identifier, scope: Scope, type: Type? = null): this(source, scope, source.getValue(), type)

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): LocalVariableDeclaration {
		return LocalVariableDeclaration(source, scope, name, type?.withTypeSubstitutions(linter, typeSubstitutions), value, isConstant,
			isMutable)
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		tracker.declare(this)
	}
}
