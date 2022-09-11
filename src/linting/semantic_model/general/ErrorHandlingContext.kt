package linting.semantic_model.general

import parsing.syntax_tree.general.StatementSection

class ErrorHandlingContext(val source: StatementSection, val mainBlock: StatementBlock, val handleBlocks: List<HandleBlock>, val alwaysBlock: StatementBlock?): Unit() {

	init {
		units.add(mainBlock)
		units.addAll(handleBlocks)
		if(alwaysBlock != null)
			units.add(alwaysBlock)
	}
}