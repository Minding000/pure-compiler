package linting.semantic_model.control_flow

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValueDeclaration
import linting.semantic_model.scopes.Scope
import components.parsing.syntax_tree.control_flow.OverGenerator as OverGeneratorSyntaxTree

class OverGenerator(override val source: OverGeneratorSyntaxTree, val collection: Value,
					val keyDeclaration: VariableValueDeclaration?, val valueDeclaration: VariableValueDeclaration?):
	Unit(source) {

	init {
		units.add(collection)
		if(keyDeclaration != null)
			units.add(keyDeclaration)
		if(valueDeclaration != null)
			units.add(valueDeclaration)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		collection.linkValues(linter, scope)
		keyDeclaration?.type = collection.type?.getKeyType(linter)
		valueDeclaration?.type = collection.type?.getValueType(linter)
	}
}
