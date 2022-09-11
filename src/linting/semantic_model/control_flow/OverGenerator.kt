package linting.semantic_model.control_flow

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValueDeclaration
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.control_flow.OverGenerator

class OverGenerator(val source: OverGenerator, val collection: Value, val keyDeclaration: VariableValueDeclaration?,
					val valueDeclaration: VariableValueDeclaration?): Unit() {

	init {
		units.add(collection)
		if(keyDeclaration != null)
			units.add(keyDeclaration)
		if(valueDeclaration != null)
			units.add(valueDeclaration)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		collection.linkValues(linter, scope)
		if(keyDeclaration != null)
			keyDeclaration.type = collection.type?.getKeyType(linter)
		if(valueDeclaration != null)
			valueDeclaration.type = collection.type?.getValueType(linter)
	}
}