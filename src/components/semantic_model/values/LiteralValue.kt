package components.semantic_model.values

import components.semantic_model.context.VariableTracker
import components.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

abstract class LiteralValue(source: SyntaxTreeNode, scope: Scope): Value(source, scope) {

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		staticValue = this
	}
}
