package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.control_flow.OverGenerator as OverGeneratorSyntaxTree

class OverGenerator(override val source: OverGeneratorSyntaxTree, val collection: Value,
					val keyDeclaration: ValueDeclaration?, val valueDeclaration: ValueDeclaration?):
	Unit(source) {

	init {
		addUnits(collection, keyDeclaration, valueDeclaration)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		collection.linkValues(linter, scope)
		//TODO should this default to Int? What about unordered collections? Get it from the super class?
		keyDeclaration?.type = collection.type?.getKeyType(linter)
		valueDeclaration?.type = collection.type?.getValueType(linter)
	}
}
