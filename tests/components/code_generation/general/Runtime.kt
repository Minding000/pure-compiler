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

	@Test
	fun `the stacktrace includes initializer calls`() {
		val sourceCode = """
			referencing Pure
			TownHall class {
				init {
					raise Exception("Whoops")
				}
			}
			Town class {
				init {
					TownHall()
				}
			}
			SimplestApp object {
				to run() {
					Town()
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Whoops
			 at Test:Test:4:TownHall()
			 at Test:Test:9:Town()
			 at Test:Test:14:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `the stacktrace includes computed property getter calls`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				computed name: String gets {
					raise Exception("Whoops")
				}
				computed id: String gets name
				to run() {
					id
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Whoops
			 at Test:Test:4:get name: String
			 at Test:Test:6:get id: String
			 at Test:Test:8:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `the stacktrace includes computed property setter calls`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				computed name: String sets {
					raise Exception("Whoops")
				}
				computed id: String sets name = id
				to run() {
					id = "1"
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Whoops
			 at Test:Test:4:set name: String
			 at Test:Test:6:set id: String
			 at Test:Test:8:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}
}
