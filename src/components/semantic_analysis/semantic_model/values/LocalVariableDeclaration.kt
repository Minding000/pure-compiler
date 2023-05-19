package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.literals.Identifier

class LocalVariableDeclaration(source: Element, scope: MutableScope, name: String, type: Type? = null, value: Value? = null,
							   isConstant: Boolean = true, isMutable: Boolean = false, isSpecificCopy: Boolean = false):
	ValueDeclaration(source, scope, name, type, value, isConstant, isMutable, isSpecificCopy) {

	constructor(source: Identifier, scope: MutableScope, type: Type? = null): this(source, scope, source.getValue(), type)

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): LocalVariableDeclaration {
		return LocalVariableDeclaration(source, scope, name, type?.withTypeSubstitutions(typeSubstitutions), value, isConstant,
			isMutable)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		tracker.declare(this)
	}
}
