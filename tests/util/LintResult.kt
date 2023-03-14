package util

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Program
import components.semantic_analysis.semantic_model.general.Unit

class LintResult(val linter: Linter, val program: Program): LogResult(linter.logger) {

	inline fun <reified T: Unit>find(noinline predicate: (T) -> Boolean = { true }): T? {
		val file = program.getFile(listOf("Test", "Test"))
		return file?.find(predicate)
	}
}
