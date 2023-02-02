package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.HandleBlock as HandleBlockSyntaxTree

class HandleBlock(override val source: HandleBlockSyntaxTree, val eventType: Type, val eventVariable: ValueDeclaration?,
				  val block: StatementBlock): Unit(source) {

	init {
		addUnits(eventType, eventVariable, block)
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		if(eventVariable != null)
			tracker.declare(eventVariable)
		block.analyseDataFlow(linter, tracker)
	}
}
