package components.code_generation.general

import org.junit.jupiter.api.Test
import util.TestApp

internal class Runtime {

	@Test
	fun `prints description of unhandled exception`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to run() {
					raise Exception("Whoops")
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		app.shouldPrintLine("Unhandled error: Whoops", "", 1)
	}
}
