package linter.elements.control_flow

import linter.Linter
import linter.elements.general.Unit
import linter.elements.values.LocalVariableDeclaration
import linter.elements.values.Value
import linter.scopes.Scope
import parsing.ast.control_flow.OverGenerator

class OverGenerator(val source: OverGenerator, val collection: Value, val keyDeclaration: LocalVariableDeclaration?,
					val valueDeclaration: LocalVariableDeclaration?): Unit() {

	init {
		units.add(collection)
		if(keyDeclaration != null)
			units.add(keyDeclaration)
		if(valueDeclaration != null)
			units.add(valueDeclaration)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		collection.linkReferences(linter, scope)
		if(keyDeclaration != null)
			keyDeclaration.type = collection.type?.getKeyType(linter)
		if(valueDeclaration != null)
			valueDeclaration.type = collection.type?.getValueType(linter)
	}
}