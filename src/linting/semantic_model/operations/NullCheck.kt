package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import parsing.syntax_tree.operations.NullCheck

class NullCheck(override val source: NullCheck, val value: Value): Value(source) {

	init {
		units.add(value)
		val booleanType = ObjectType(source, Linter.LiteralType.BOOLEAN.className)
		units.add(booleanType)
		type = booleanType
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		for(unit in units)
			if(unit != type)
				unit.linkTypes(linter, scope)
		linter.link(Linter.LiteralType.BOOLEAN, type)
	}
}