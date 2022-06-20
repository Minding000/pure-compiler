package linter.elements.definitions

import linter.Linter
import linter.elements.literals.Type
import linter.elements.values.VariableValueDeclaration
import linter.scopes.MutableScope
import parsing.ast.definitions.Parameter

class Parameter(override val source: Parameter, name: String, type: Type?, isMutable: Boolean, hasDynamicSize: Boolean):
	VariableValueDeclaration(source, name, type, true) {

	init {
		if(type != null)
			units.add(type)
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		if(type == null)
			type = scope.resolveValue(name)?.type
	}
}