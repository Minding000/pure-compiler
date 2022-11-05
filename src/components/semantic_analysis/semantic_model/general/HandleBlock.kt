package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import components.syntax_parser.syntax_tree.general.HandleBlock as HandleBlockSyntaxTree

class HandleBlock(override val source: HandleBlockSyntaxTree, val eventType: Type,
				  val eventVariable: VariableValueDeclaration?, val block: StatementBlock): Unit(source) {

	init {
		addUnits(eventType, eventVariable, block)
	}
}
