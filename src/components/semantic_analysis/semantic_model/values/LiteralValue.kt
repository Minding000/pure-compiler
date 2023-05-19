package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element

abstract class LiteralValue(source: Element, scope: Scope): Value(source, scope) {

	override fun determineTypes() {
		super.determineTypes()
		staticValue = this
	}

	override fun getComputedValue(tracker: VariableTracker): Value? {
		return this
	}
}
