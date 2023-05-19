package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

abstract class LiteralValue(source: SyntaxTreeNode, scope: Scope): Value(source, scope) {

	override fun determineTypes() {
		super.determineTypes()
		staticValue = this
	}

	override fun getComputedValue(tracker: VariableTracker): Value? {
		return this
	}
}
