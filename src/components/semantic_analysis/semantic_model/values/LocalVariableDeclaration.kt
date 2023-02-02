package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.literals.Identifier

class LocalVariableDeclaration(source: Element, name: String, type: Type? = null, value: Value? = null, isConstant: Boolean = true,
							   isMutable: Boolean = false): ValueDeclaration(source, name, type, value, isConstant, isMutable) {

	constructor(source: Identifier, type: Type? = null): this(source, source.getValue(), type)

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): LocalVariableDeclaration {
		return LocalVariableDeclaration(source, name, type?.withTypeSubstitutions(typeSubstitutions), value, isConstant, isMutable)
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		tracker.declare(this)
	}
}
