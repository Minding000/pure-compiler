package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import parsing.syntax_tree.operations.NullCheck

class NullCheck(override val source: NullCheck, val value: Value): Value(source) {

	init {
		units.add(value)
		type = ObjectType(source, Linter.Literals.BOOLEAN)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, scope)
		linter.booleanLiteralScope?.let { literalScope -> type?.linkTypes(linter, literalScope) }
	}
}