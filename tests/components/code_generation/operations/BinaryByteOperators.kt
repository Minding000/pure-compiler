package components.code_generation.operations

import org.junit.jupiter.api.Test
import util.TestApp
import util.TestUtil
import kotlin.test.assertEquals

internal class BinaryByteOperators {

	@Test
	fun `compiles byte additions`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Byte {
					val left: Byte = 2
					val right: Byte = 3
					return left + right
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `compiles byte subtractions`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Byte {
					val left: Byte = 8
					val right: Byte = 3
					return left - right
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `compiles byte multiplications`() {
		val sourceCode = """
			SimplestApp object {
				to getTen(): Byte {
					val left: Byte = 5
					val right: Byte = 2
					return left * right
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getTen")
		assertEquals(10, result)
	}

	@Test
	fun `compiles byte divisions`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Byte {
					val left: Byte = 10
					val right: Byte = 2
					return left / right
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `throws on division by zero`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to run() {
					val left: Byte = 10
					val right: Byte = 0
					left / right
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Division by zero
			 at Test:Test:6:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `throws on overflowing division`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to run() {
					val left: Byte = -128
					val right: Byte = -1
					left / right
				}
			}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.run")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Division overflowed
			 at Test:Test:6:SimplestApp.run()
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `compiles byte smaller than`() {

		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					val left: Byte = 4
					val right: Byte = 4
					return left < right
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, result)
	}

	@Test
	fun `compiles byte greater than`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					val left: Byte = 4
					val right: Byte = 4
					return left > right
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, result)
	}

	@Test
	fun `compiles byte smaller than or equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					val left: Byte = 4
					val right: Byte = 4
					return left <= right
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, result)
	}

	@Test
	fun `compiles byte greater than or equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					val left: Byte = 4
					val right: Byte = 4
					return left >= right
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, result)
	}

	@Test
	fun `compiles byte equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					val left: Byte = 4
					val right: Byte = 4
					return left == right
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, result)
	}

	@Test
	fun `compiles byte not equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					val left: Byte = 4
					val right: Byte = 4
					return left != right
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, result)
	}
}
