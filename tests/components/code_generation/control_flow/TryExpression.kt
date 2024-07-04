package components.code_generation.control_flow

import org.junit.jupiter.api.Test
import util.TestApp
import util.TestUtil
import kotlin.test.assertEquals

internal class TryExpression {

	@Test
	fun `unchecked try doesn't catch error`() {
		val sourceCode = """
			SimplestApp object {
				to throw() {
					raise 23
				}
				to run() {
					try! throw()
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.shouldPrint("Unhandled error at '0000000000000017'.${System.lineSeparator()}", "", 1)
	}

	@Test
	fun `unchecked try forwards return value`() {
		val sourceCode = """
			SimplestApp object {
				to compute(): Int {
					return 84
				}
				to getEightyFour(): Int {
					return try! compute()
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyFour")
		assertEquals(84, result)
	}

	@Test
	fun `optional try catches error in function without return value`() {
		val sourceCode = """
			SimplestApp object {
				to throw() {
					raise 23
				}
				to run() {
					try? throw()
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.shouldPrint("")
	}

	@Test
	fun `optional try returns null for error in function with return value`() {
		val sourceCode = """
			SimplestApp object {
				to throw(): Int {
					raise 23
				}
				to getThirtyFour(): Int {
					val a = try? throw()
					return a ?? 34
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThirtyFour")
		assertEquals(34, result)
	}

	@Test
	fun `optional try forwards return value`() {
		val sourceCode = """
			SimplestApp object {
				val a = 84
				to compute(): SimplestApp {
					return SimplestApp
				}
				to getEightyFour(): Int {
					val b = try? compute()
					return b?.a ?? 5
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyFour")
		assertEquals(84, result)
	}

	@Test
	fun `optional try boxes return value`() {
		val sourceCode = """
			SimplestApp object {
				to compute(): Int {
					return 84
				}
				to getEightyFour(): Int {
					val b = try? compute()
					return b ?? 5
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyFour")
		assertEquals(84, result)
	}
}
