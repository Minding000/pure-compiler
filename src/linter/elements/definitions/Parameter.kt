package linter.elements.definitions

import linter.Linter
import linter.elements.literals.Type
import linter.elements.values.VariableValueDeclaration
import linter.scopes.MutableScope
import parsing.ast.definitions.Parameter as ASTParameter

class Parameter(override val source: ASTParameter, name: String, type: Type?, val isMutable: Boolean,
				val hasDynamicSize: Boolean): VariableValueDeclaration(source, name, type, null, true) {

	init {
		if(type != null)
			units.add(type)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<Type, Type>): Parameter {
		return Parameter(source, name, type?.withTypeSubstitutions(typeSubstitution), isMutable, hasDynamicSize)
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		if(type == null)
			type = scope.resolveValue(name)?.type
	}
}