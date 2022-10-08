package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.literals.Type
import linting.semantic_model.general.Unit
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.literals.Identifier

open class VariableValueDeclaration(open val source: Element, val name: String, var type: Type?, val value: Value?,
									val isConstant: Boolean = true, val isMutable: Boolean = false): Unit() {

	init {
		type?.let { type ->
			units.add(type)
		}
		if(value != null)
			units.add(value)
	}

	constructor(source: Identifier): this(source, source.getValue(), null, null, true, true)

	open fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): VariableValueDeclaration {
		return VariableValueDeclaration(source, name, type?.withTypeSubstitutions(typeSubstitution), value, isConstant,
			isMutable)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		if(type == null)
			type = value?.type
	}
}