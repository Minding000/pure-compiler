package components.semantic_analysis.semantic_model.general

import components.syntax_parser.syntax_tree.general.StatementSection

class ErrorHandlingContext(override val source: StatementSection, val mainBlock: StatementBlock,
						   val handleBlocks: List<HandleBlock>, val alwaysBlock: StatementBlock?): Unit(source) {

	init {
		addUnits(mainBlock, alwaysBlock)
		addUnits(handleBlocks)
	}
}
