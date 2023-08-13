package util

import components.semantic_analysis.semantic_model.context.Context
import components.semantic_analysis.semantic_model.general.Program
import components.semantic_analysis.semantic_model.general.SemanticModel

class LintResult(val context: Context, val program: Program): LogResult(context.logger) {

	inline fun <reified T: SemanticModel>find(noinline predicate: (T) -> Boolean = { true }): T? {
		val file = program.getFile(listOf(TestUtil.TEST_MODULE_NAME, TestUtil.TEST_FILE_NAME))
		return file?.find(predicate)
	}
}
