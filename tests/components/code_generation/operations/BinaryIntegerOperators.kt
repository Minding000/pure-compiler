package components.code_generation.operations

import org.junit.jupiter.api.Test
import util.TestApp
import util.TestUtil
import kotlin.test.assertEquals

internal class BinaryIntegerOperators {
	//TODO implement unsigned integers

	@Test
	fun `compiles integer additions`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					return 2 + 3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `compiles integer subtractions`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					return 8 - 3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `compiles integer multiplications`() {
		val sourceCode = """
			SimplestApp object {
				to getTen(): Int {
					return 5 * 2
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTen")
		assertEquals(10, result)
	}

	@Test
	fun `compiles integer divisions`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					return 10 / 2
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `throws on overflowing division`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					-2147483648 / -1
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Division overflowed
			 at Test:Test:3:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `throws on division by zero`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					7 / 0
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Division by zero
			 at Test:Test:3:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `compiles integer smaller than`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return 4 < 4
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, result)
	}

	@Test
	fun `compiles integer greater than`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return 4 > 4
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, result)
	}

	@Test
	fun `compiles integer smaller than or equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return 4 <= 4
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, result)
	}

	@Test
	fun `compiles integer greater than or equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return 4 >= 4
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, result)
	}

	@Test
	fun `compiles integer equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return 4 == 4
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, result)
	}

	@Test
	fun `compiles integer not equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return 4 != 4
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, result)
	}
}
