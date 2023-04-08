package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.HandleBlock as HandleBlockSyntaxTree

class HandleBlock(override val source: HandleBlockSyntaxTree, scope: Scope, val eventType: Type, val eventVariable: ValueDeclaration?,
				  val block: StatementBlock): Unit(source, scope) {

	init {
		addUnits(eventType, eventVariable, block)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(eventVariable != null)
			tracker.declare(eventVariable, true)
		block.analyseDataFlow(tracker)
	}
}
