package components.code_generation.general

import org.junit.jupiter.api.Test
import util.TestApp

internal class Runtime {

	@Test
	fun `prints description and stack trace of unhandled exceptions`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to throw() {
					raise Exception("Whoops")
				}
				to run() {
					throw()
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Whoops
			 at Test:Test:4:SimplestApp.throw()
			 at Test:Test:7:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	//TODO
	// - implement String(Int)
	// - add native implementation for Identifiable.stringRepresentation
	// - ensure computed property without getter produces an error
}
