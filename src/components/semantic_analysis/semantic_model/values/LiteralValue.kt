package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element

abstract class LiteralValue(source: Element, scope: Scope): Value(source, scope) {

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		staticValue = this
	}

	override fun getComputedLiteralValue(tracker: VariableTracker): LiteralValue {
		return this
	}
}
