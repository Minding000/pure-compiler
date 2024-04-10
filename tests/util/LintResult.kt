package util

import components.semantic_model.context.Context
import components.semantic_model.general.File
import components.semantic_model.general.Program
import components.semantic_model.general.SemanticModel
import kotlin.test.assertNotNull

class LintResult(val context: Context, val program: Program): LogResult(context.logger) {

	inline fun <reified T: SemanticModel>find(noinline predicate: (T) -> Boolean = { true }): T? {
		return find(TestUtil.TEST_FILE_NAME, predicate)
	}

	inline fun <reified T: SemanticModel>find(fileName: String, noinline predicate: (T) -> Boolean = { true }): T? {
		return getFile(fileName).find(predicate)
	}

	fun getFile(name: String): File {
		val file = program.getFile(listOf(TestUtil.TEST_MODULE_NAME, name))
		assertNotNull(file)
		return file
	}
}
