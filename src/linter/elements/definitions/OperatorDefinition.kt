package linter.elements.definitions

import linter.Linter
import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.elements.values.VariableValueDeclaration
import linter.scopes.BlockScope
import linter.scopes.Scope
import parsing.ast.definitions.OperatorDefinition

class OperatorDefinition(override val source: OperatorDefinition, name: String, val scope: BlockScope,
						 val parameters: List<Parameter>, val body: Unit?, val returnType: Type?):
	VariableValueDeclaration(source, name, returnType, true) {
	val variation = parameters.joinToString { parameter -> parameter.type.toString() }

	init {
		units.addAll(parameters)
		if(body != null)
			units.add(body)
		if(returnType != null)
			units.add(returnType)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, this.scope)
	}

	override fun toString(): String {
		return "$name($variation)"
	}
}