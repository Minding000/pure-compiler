package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element

abstract class LiteralValue(source: Element): Value(source) {

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		staticValue = this
	}
}
