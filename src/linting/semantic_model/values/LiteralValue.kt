package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.general.Element

abstract class LiteralValue(source: Element): Value(source) {

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		staticValue = this
	}
}
