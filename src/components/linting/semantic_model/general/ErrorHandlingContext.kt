package components.linting.semantic_model.general

import components.syntax_parser.syntax_tree.general.StatementSection

class ErrorHandlingContext(override val source: StatementSection, val mainBlock: StatementBlock,
						   val handleBlocks: List<HandleBlock>, val alwaysBlock: StatementBlock?): Unit(source) {

	init {
		units.add(mainBlock)
		units.addAll(handleBlocks)
		if(alwaysBlock != null)
			units.add(alwaysBlock)
	}
}
