package components.code_generation.control_flow

import org.junit.jupiter.api.Test
import util.TestApp

internal class RaiseStatement {

	@Test
	fun `compiles raise in function without return type`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					raise 23
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.shouldPrintLine("Unhandled error at '0000000000000017'.", "", 1)
	}

	@Test
	fun `compiles raise in function with object return type`() {
		val sourceCode = """
			Drawer class
			SimplestApp object {
				to run(): Drawer {
					raise 23
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.shouldPrintLine("Unhandled error at '0000000000000017'.", "", 1)
	}

	@Test
	fun `compiles raise in function with boolean return type`() {
		val sourceCode = """
			SimplestApp object {
				to run(): Bool {
					raise 23
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.shouldPrintLine("Unhandled error at '0000000000000017'.", "", 1)
	}

	@Test
	fun `compiles raise in function with byte return type`() {
		val sourceCode = """
			SimplestApp object {
				to run(): Byte {
					raise 23
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.shouldPrintLine("Unhandled error at '0000000000000017'.", "", 1)
	}

	@Test
	fun `compiles raise in function with integer return type`() {
		val sourceCode = """
			SimplestApp object {
				to run(): Int {
					raise 23
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.shouldPrintLine("Unhandled error at '0000000000000017'.", "", 1)
	}

	@Test
	fun `compiles raise in function with float return type`() {
		val sourceCode = """
			SimplestApp object {
				to run(): Float {
					raise 23
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.shouldPrintLine("Unhandled error at '0000000000000017'.", "", 1)
	}
}
