package linter.elements.values

import linter.Linter
import linter.elements.literals.Type
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.literals.Identifier

open class VariableValueDeclaration(open val source: Element, val name: String, var type: Type?, val value: Value?,
									val isConstant: Boolean): Unit() {

	init {
		type?.let {
			units.add(it)
		}
		if(value != null)
			units.add(value)
	}

	constructor(source: Identifier): this(source, source.getValue(), null, null, true)

	open fun withTypeSubstitutions(typeSubstitution: Map<Type, Type>): VariableValueDeclaration {
		return VariableValueDeclaration(source, name, type?.withTypeSubstitutions(typeSubstitution), value, isConstant)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		if(type == null)
			type = value?.type
	}
}