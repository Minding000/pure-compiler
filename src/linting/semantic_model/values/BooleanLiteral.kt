package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.literals.BooleanLiteral

class BooleanLiteral(override val source: BooleanLiteral, val value: Boolean): LiteralValue(source) {

	init {
		val type = ObjectType(source, Linter.Literals.BOOLEAN)
		units.add(type)
		this.type = type
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		linter.booleanLiteralScope?.let { literalScope -> super.linkTypes(linter, literalScope) }
	}
}